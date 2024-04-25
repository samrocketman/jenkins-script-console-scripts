/*
    Copyright (c) 2015-2024 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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

Map repos = Jenkins.instance.getAllItems(WorkflowMultiBranchProject).findAll {
    it.sources.find { scm ->
        scm.source in GitHubSCMSource
    }
}.collect { job ->
    job.sources.find { scm ->
        scm.source in GitHubSCMSource
    }.source.with {
        [
            (job.fullName): [
                owner: it.repoOwner,
                name: it.repository,
                repoWithOwner: it.repoOwner + '/' + it.repository
            ]
        ]
    }
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
query {<% int i = 0; repos.each { nameWithOwner, repo -> %>
  repo${i++}: repository(owner: "${repo.owner}", name: "${repo.name}") {
    nameWithOwner
    isArchived
  }<% } %>
}
'''.trim()

List queries = groupBy100.collect { k, v -> getScriptFromTemplate(graphql_template, [repos: v]) }
List archivedRepos = []

GitHubGraphQL github = new GitHubGraphQL()
github.credential = new JenkinsTokenCredential('github-token')
limit = 300
count = 0
queries.each { String query ->
    int retryWait = 3
    while(true) {
        try {
            github.sendGQL(query).data.each { k, repo ->
                if(repo.isArchived) {
                    archivedRepos << repo.nameWithOwner
                }
            }
            break
        } catch(Exception e) {
            count++
            if(count > limit) {
                // Useful if we will indefinitely encounter exceptions.
                // Retrying GraphQL requests should be relatively rare.
                throw e
            }
            int sleepInterval = (new Random()).nextInt(retryWait) + 1
            println("Will try GitHub communication again after sleeping for ${sleepInterval} seconds.")
            sleep(retryWait*1000)
            retryWait = (retryWait + 3) % 100
            //retry request indefinitely changing up random sleep intervals
        }
    }
}
println("${(dryRun ? 'DRYRUN: ' : '')}Number of jobs related to archived repositories: ${archivedRepos.size()}")
List jenkinsJobsToDelete = repos.findAll { k, v ->
    v.repoWithOwner in archivedRepos
}.collect { k, v -> k }
println("    ${jenkinsJobsToDelete.join('\n    ')}")

if(!dryRun) {
    jenkinsJobsToDelete.each {
        Jenkins.instance.getItemByFullName(it).delete()
        println("Deleted job ${it}")
    }
}
if(!dryRun) {
    println("${jenkinsJobsToDelete.size()} jenkins jobs have been deleted related to archived GitHub repositories.")
}
else {
    println 'Nothing was deleted because dryRun is enabled.'
}
