/*
    Copyright (c) 2015-2017 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   This script will kill all in-progress builds associated with a Jenkins job.
*/
import hudson.model.FreeStyleBuild
import hudson.model.Result
import hudson.model.Run
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowRun

if(!binding.hasVariable('dryRun')) {
    dryRun = true
}
if(!binding.hasVariable('projectFullName')) {
    projectFullName = 'folder/project'
}

Jenkins.instance.getItemByFullName(projectFullName).builds.each { Run item ->
    if(item.isBuilding()) {
        if(item instanceof WorkflowRun) {
            WorkflowRun run = (WorkflowRun) item
            if(!dryRun) {
                run.doKill()
            }
            println "Killed ${run}"
        } else if(item instanceof FreeStyleBuild) {
            FreeStyleBuild run = (FreeStyleBuild) item
            if(!dryRun) {
                run.executor.interrupt(Result.ABORTED)
            }
            println "Killed ${run}"
        } else {
            println "WARNING: Don't know how to handle ${item.class}"
        }
    }
}.each{ build ->
  if(build.isBuilding()) {
    build.doKill()
    println "killed ${build}"
  }
}

//null so no result shows up
null
