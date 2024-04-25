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
/*
   This script will search a Jenkins instance for all freestyle jobs, pipeline
   jobs, or multibranch pipeline jobs which have the repository configured to
   be cloned.  This script is useful for discovering jobs which might affect a
   given repository.
 */

// enter the repository to search
String repository = 'namespace/example-repository'

// core classes
import hudson.model.Job
import hudson.model.Item
import hudson.scm.NullSCM
import jenkins.model.Jenkins
import java.util.regex.Pattern
import hudson.model.FreeStyleProject

/*
   The following commented out classes are here because we use them, but they
   are provided by a plugin which may not be installed in a Jenkins instance.
   They're discovered without importing them.
   */
//import org.jenkinsci.plugins.multiplescms.MultiSCM
//import org.jenkinsci.plugins.workflow.job.WorkflowJob
//import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

Pattern pattern = Pattern.compile(".*\\Q${repository}\\E(\\.git)?\$")

List<String> getMatchingMultibranchPipelineUrls(Pattern p) {
    Jenkins.instance.getAllItems(Item).findAll { Item j ->
        j.class.simpleName == 'WorkflowMultiBranchProject'
    }.findAll { Item j ->
        j.getSCMSources().find {
            it.class.simpleName == 'GitHubSCMSource'
        }?.with {
            p.matcher("${it.repoOwner}/${it.repository}").matches()
        }
    }.collect { Item j ->
        "${Jenkins.instance.rootUrl}${j.url}"
    }
}

List<String> getMatchingStandalonePipelineUrls(Pattern p) {
    Jenkins.instance.getAllItems(Job).findAll { Job j ->
        j.class.simpleName == 'WorkflowJob' &&
        j.parent.class.simpleName != 'WorkflowMultiBranchProject'
    }.findAll { Job j ->
        j.definition?.scm?.userRemoteConfigs*.url.any { String s ->
            p.matcher(s).matches()
        }
    }.collect { Job j ->
        "${Jenkins.instance.rootUrl}${j.url}"
    }
}

List<String> getMatchingFreeStyleJobUrls(Pattern p) {
    Jenkins.instance.getAllItems(FreeStyleProject).findAll { Job j ->
        if(j.scm in NullSCM) {
            return false
        }
        j.scm.with { scm ->
            if(scm.class.simpleName == 'MultiSCM') {
                return scm.configuredSCMs*.userRemoteConfigs.flatten()
            }
            j.scm?.userRemoteConfigs ?: []
        }*.url.any { String s ->
            p.matcher(s).matches()
        }
    }.collect { Job j ->
        "${Jenkins.instance.rootUrl}${j.url}"
    }
}

List<String> findAllMatchingJobUrls(Pattern p) {
    [
        getMatchingFreeStyleJobUrls(p),
        getMatchingMultibranchPipelineUrls(p),
        getMatchingStandalonePipelineUrls(p)
    ].flatten().sort()
}

'\n* ' + (findAllMatchingJobUrls(pattern).join('\n* ') ?: 'No jobs found')
