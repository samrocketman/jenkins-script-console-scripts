/*
    Copyright (c) 2015-2018 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   This script kills all builds which have been running longer than one day.
   This helps to keep a Jenkins instance healthy and executors free from
   existing stale build runs.

   Supported types to kill:
       FreeStyleBuild
       WorkflowRun (Jenkins Pipelines)
*/

import hudson.model.FreeStyleBuild
import hudson.model.Job
import hudson.model.Result
import hudson.model.Run
import java.util.Calendar
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.StageStepExecution

//24 hours in a day, 3600 seconds in 1 hour, 1000 milliseconds in 1 second
long time_in_millis = 24*3600*1000
Calendar rightNow = Calendar.getInstance()

Jenkins.instance.getAllItems(Job.class).findAll { Job job ->
    job.isBuilding()
}.collect { Job job ->
    //find all matching items and return a list but if null then return an empty list
    job.builds.findAll { Run run ->
        run.isBuilding() && ((rightNow.getTimeInMillis() - run.getStartTimeInMillis()) > time_in_millis)
    } ?: []
}.sum().each { Run item ->
    if(item in WorkflowRun) {
        WorkflowRun run = (WorkflowRun) item
        //hard kill
        run.doKill()
        //release pipeline concurrency locks
        StageStepExecution.exit(run)
        println "Killed ${run}"
    } else if(item in FreeStyleBuild) {
        FreeStyleBuild run = (FreeStyleBuild) item
        run.executor.interrupt(Result.ABORTED)
        println "Killed ${run}"
    } else {
        println "WARNING: Don't know how to handle ${item.class}"
    }
}

//null means there will be no return result for the script
null
