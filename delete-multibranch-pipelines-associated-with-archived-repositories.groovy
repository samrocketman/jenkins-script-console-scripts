/*
    Copyright (c) 2015-2026 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
    the Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
/**
  Find all multibranch pipeline jobs which are configured with GitHub branch
  source.  Generate GraphQL queries to query GitHub repository archived status.
  For every job associated with an archived GitHub repository, delete the
  multibranch pipeline job.

  By default will only print jobs which would be deleted.

  Assumes you have the following plugins installed:
    - GitHub Branch Source Plugin
    - Pipeline: Multibranch Plugin
    - SCM Filter Jervis Plugin
  */
import static net.gleske.jervis.tools.AutoRelease.getScriptFromTemplate
import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import hudson.security.ACL
import jenkins.model.Jenkins
import net.gleske.jervis.remotes.GitHubGraphQL
import net.gleske.jervis.remotes.interfaces.TokenCredential
import org.jenkinsci.plugins.plaincredentials.StringCredentials
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource

// if false will really delete jobs
Boolean dryRun = true

class JenkinsTokenCredential implements TokenCredential {
    private String credentials_id
    JenkinsTokenCredential(String id) {
        this.credentials_id = id
    }
    String getToken() {
        List<StringCredentials> credentials = CredentialsProvider.lookupCredentials(StringCredentials, Jenkins.getInstance(), ACL.SYSTEM)
        StringCredentials found_credentials = CredentialsMatchers.firstOrNull(credentials, CredentialsMatchers.withId(this.credentials_id))
        if(!found_credentials) {
            throw new Exception("ERROR: could not find Jenkins string credential with ID ${this.credentials_id}")
        }
        found_credentials.secret.plainText
    }
    void setToken(String s) {
        //discard
        null
    }
}

// Multiple Jenkins jobs may point at the same GitHub repository.  This
// structure keys on repoWithOwner so that repository archive status is queried
// only once but known for all jobs referencing the repository in Jenkins.
Map repos = [:].withDefault { [] }
Jenkins.instance.getAllItems(WorkflowMultiBranchProject).findAll { it.sources }.each { job ->
    job.sources.first().source.with {
        String repoWithOwner = it.repoOwner + '/' + it.repository
        Map entry = [(job.fullName): [owner: it.repoOwner, name: it.repository, repoWithOwner: repoWithOwner]]
        repos[repoWithOwner] << entry
    }
}

// Flatten the list of per-job metadata maps into a single map of
// jobFullName -> metadata for each repository.
//   repos = [ 'owner/name': [ jobFullName: [owner, name, repoWithOwner], ... ], ... ]
repos = repos.collect { k, v ->
    [(k): v.sum()]
}.sum()

Map groupBy100 = [:]
int limit = 100
int group = 0
int count = 0
repos.each { k, v ->
    if(!(count++ % limit)) {
        group++
    }
    if(!groupBy100[group]) {
        groupBy100[group] = [:]
    }
    groupBy100[group][(k)] = v
}

String graphql_template = '''
query {<% int i = 0; repos.each { nameWithOwner, jobs -> def repo = jobs.values().first() %>
  repo${i++}: repository(owner: "${repo.owner}", name: "${repo.name}") {
    nameWithOwner
    isArchived
  }<% } %>
}
'''.trim()

List queries = groupBy100.collect { k, v -> getScriptFromTemplate(graphql_template, [repos: v]) }
List archivedRepos = []
Map queryRepoResult = [:]

GitHubGraphQL github = new GitHubGraphQL()
github.credential = new JenkinsTokenCredential('github-token')
queries.each { String query ->
    int retryWait = 3
    int retryCurrent = 0
    int retryMax = 10
    int sleepInterval = 0
    while(true) {
        try {
            Map response = github.sendGQL(query)
            if(response.errors) {
                retryCurrent++
                if(retryCurrent >= retryMax) {
                    throw new Exception("\n\nMax retry reached: GraphQL errors were encountered.\n\n${response.errors}\n\n")
                }
                sleepInterval = (new Random()).nextInt(retryWait) + 1
                println("Will try GitHub communication again after sleeping for ${sleepInterval} seconds.")
                Thread.sleep(retryWait*1000)
                retryWait = (retryWait + 3) % 100
                // retry sendGQL for this query by restarting the while loop.
                continue
            }
            response.data.each { k, repo ->
                if(!repo) {
                    return
                }
                queryRepoResult[repo.nameWithOwner] = repo.isArchived
                if(repo.isArchived) {
                    archivedRepos << repo.nameWithOwner
                }
            }
            break
        } catch(Exception e) {
            retryCurrent++
            if(retryCurrent >= retryMax) {
                throw e
            }
            sleepInterval = (new Random()).nextInt(retryWait) + 1
            println("Will try GitHub communication again after sleeping for ${sleepInterval} seconds.")
            Thread.sleep(retryWait*1000)
            retryWait = (retryWait + 3) % 100
            //retry request to a limit changing up random sleep intervals
        }
    }
}

// Collect repositories which returned null from GraphQL
// they must also be deleted, as they were either deleted or renamed.
archivedRepos += (repos.keySet() - queryRepoResult.keySet()).toList()

// Collect every job (across all archived/missing repositories) to delete.
//   jenkinsJobsToDelete = [ jobFullName: [owner, name, repoWithOwner], ... ]
Map jenkinsJobsToDelete = [:]
repos.findAll { k, v ->
    k in archivedRepos
}.each { k, v ->
    jenkinsJobsToDelete << v
}

println("Number of Jenkins jobs to delete: ${jenkinsJobsToDelete.size()}")
jenkinsJobsToDelete.each { String jobFullName, Map meta ->
    if(!dryRun) {
        Jenkins.instance.getItemByFullName(jobFullName).delete()
    }
    println("* ${(dryRun) ? 'DRYRUN: ' : ''}Deleted job ${jobFullName} with GitHub URL https://github.com/${meta.repoWithOwner}")
}
println("${(dryRun) ? 'DRYRUN: ' : ''}${jenkinsJobsToDelete.size()} jenkins jobs have been deleted related to archived, renamed, or offboarded GitHub repositories.")
