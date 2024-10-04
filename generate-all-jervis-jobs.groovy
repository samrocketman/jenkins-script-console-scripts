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
  This script searches all jobs in a Jenkins instance and regenerates them by
  building the Jervis generator job with parameters.  It will automatically
  sort all projects by their recent builds and proceed from newest to oldest
  built projects.  Useful for migrating Job DSL script changes as Jenkins is
  upgraded or Jervis is changed.

  The recommended way to operate this script is from a freestyle Jenkins job
  configured with an "Execute system Groovy script" build step.
  */
import hudson.model.Item
import hudson.model.ParametersAction
import hudson.model.StringParameterValue
import jenkins.model.Jenkins
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

/*
   USER CONFIGURABLE VARIABLES
 */

// Default 0 seconds between retries.  However, if you're regenerating a large
// number of builds then you'll want to avoid API limits by throttling how
// quickly jobs get generated.  If you need to throttle, then a recommended
// value is 20 seconds between jobs being generated.
Integer secondsToWaitBetweenBuilds = 20

// Optional: update this list of matching names and only projects which match a
// given set of names will be included in the job generation.  This is useful
// if you want to isolate a set of projects by their job or folder name.  A
// matching name is an exact match or a partial name where the project starts
// with the characters.
List<String> matchingNames = []

/*
   FUNCTIONS
 */

Integer getLastBuildTimestamp(Item item) {
  (item?.builds?.find { it }?.timestamp?.toInstant()?.epochSecond) ?: 0
}

List<Item> sortBranchesByLastBuild(WorkflowMultiBranchProject project) {
  project.items.sort { a, b ->
    Integer lastBuildA = getLastBuildTimestamp(a)
    Integer lastBuildB = getLastBuildTimestamp(b)
    if(!lastBuildA && !lastBuildB) {
      b.builds.size() <=> a.builds.size()
    } else {
      lastBuildB <=> lastBuildA
    }
  }
}

List<WorkflowMultiBranchProject> sortProjectsByLastBuilt(List<WorkflowMultiBranchProject> projects) {
  projects.sort { a, b ->
    Integer lastBuildA = getLastBuildTimestamp(sortBranchesByLastBuild(a).find { it })
    Integer lastBuildB = getLastBuildTimestamp(sortBranchesByLastBuild(b).find { it })
    if(!lastBuildA && !lastBuildB) {
      b.items.size() <=> a.items.size()
    } else {
      lastBuildB <=> lastBuildA
    }
  }
}

/*
   MAIN CODE
 */

def generator_job = Jenkins.instance.getItemByFullName('_jervis_generator')
Integer milliSecondsToWait = secondsToWaitBetweenBuilds * 1000
if(milliSecondsToWait && !binding.hasVariable('out')) {
    throw new Exception('You must run this script from a freestyle job configured with "Execute system Groovy script"')
}

sortProjectsByLastBuilt(Jenkins.instance.getAllItems(WorkflowMultiBranchProject).findAll { project ->
    !matchingNames ||
    project.fullName.tokenize('/').any { path ->
        matchingNames.any { expr ->
            path.startsWith(expr)
        }
    }
}).collect { project ->
    project.getSCMSources().find { source ->
        source in GitHubSCMSource
    }.getRepositoryUrl() -~ '^https://github.com/'
}.each { String project ->
    def actions = new ParametersAction([new StringParameterValue('project', project)])
    generator_job.scheduleBuild2(0, actions)
    if(binding.hasVariable('out')) {
        out.println "Scheduled project ${project}."
        if(milliSecondsToWait) {
            out.println "Sleeping for ${secondsToWaitBetweenBuilds} seconds..."
        }
        sleep(milliSecondsToWait)
    }
    else {
        println "Scheduled project ${project}."
    }
}
