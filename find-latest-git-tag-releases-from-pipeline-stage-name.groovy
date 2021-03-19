/*
    Copyright (c) 2015-2021 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
  Search all multibranch pipelines and return the latest tag release.  If the
  build was successful and the pipeline contains a certain stage name, then it
  counts as a tag release build.

  To create this script I referenced the following source code for hints of
  available Jenkins APIs.

  https://gist.github.com/GuillaumeSmaha/fdef2088f7415c60adf95d44073c3c88
 */
import hudson.model.Result
import jenkins.model.Jenkins
import jenkins.plugins.git.GitTagSCMHead
import org.jenkinsci.plugins.workflow.actions.LabelAction
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode
import org.jenkinsci.plugins.workflow.graph.FlowNode
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty

/**
  User configurable variables.
  */

// a Git tag build must contain the following pipeline stage in order to be
// considered a release build.
String releaseStageName = 'Release to Nexus'

/**
  Get the human readable Jenkins pipeline stages if given a build the current
  flow heads from a build.  For example, build.execution.currentHeads.

  @param flowNodes A list of flow nodes to recursively search for the display
                   name of stages.
  @param stages    An optional argument used in recursion.  This argument is
                   used internally and there's no need for you to pass a list
                   at all.
  @return A List of stage display names from a build.

  */
List<String> getBuildStages(List<FlowNode> flowNodes, List<String> stages = []) {
    if(!flowNodes) {
        return stages.reverse()
    }
    flowNodes.each { FlowNode flowNode ->
        if(!(flowNode in StepEndNode)) {
            return
        }
        if(!flowNode.startNode.getAction(LabelAction)) {
            return
        }
        stages << flowNode.startNode.displayName
    }
    getBuildStages(flowNodes[-1]?.parents, stages)
}

/**
  Find a job containing a successful build whose pipeline contains a specific stage name.

  @param job       A Jenkins pipeline job object.
  @param stageName A stage name the pipeline job must contain.

  @return          A build which was the result of a successful release.
  */
WorkflowRun getSuccessfulReleaseBuild(WorkflowJob job, String stageName) {
    job.builds.find { WorkflowRun build ->
        (build?.result == Result.SUCCESS) &&
        (stageName in getBuildStages(build.execution.currentHeads))
    }
}

Map latestSuccessfulReleases = [:]

// Find the latest successful Git tag releases for all jobs that contain a
// 'Release to Nexus' stage name in its most recent build.
Jenkins.instance.getAllItems(WorkflowJob).findAll { WorkflowJob job ->
    WorkflowRun build = job.builds?.first()
    (job?.getProperty(BranchJobProperty)?.branch?.head in GitTagSCMHead) &&
    getSuccessfulReleaseBuild(job, releaseStageName)
}.sort { WorkflowJob job ->
    WorkflowRun build = getSuccessfulReleaseBuild(job, releaseStageName)
    // sort by time a job ended
    build.startTimeInMillis + build.duration
}.each { WorkflowJob job ->
    if(job.name in latestSuccessfulReleases.keySet()) {
        return
    }
    latestSuccessfulReleases[job.parent.name] = job.name
}

// print the latest successful releases
latestSuccessfulReleases.each { k, v ->
      println "${k} ${v}"
}
null
