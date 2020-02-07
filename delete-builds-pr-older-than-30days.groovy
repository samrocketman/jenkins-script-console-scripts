/*
    Copyright (c) 2015-2020 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   Delete pr builds older than 30 days on a Jenkins instance using multibranch
   pipelines inside of folders.
 */
import com.cloudbees.hudson.plugins.folder.Folder
import java.util.Date
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

long DAY_IN_MS = 1000 * 60 * 60 * 24
int days = 30
Date thirtyDaysAgo = new Date(System.currentTimeMillis() - (days * DAY_IN_MS))


Jenkins.instance.getAllItems(WorkflowJob).findAll {
    it?.parent in WorkflowMultiBranchProject &&
    it?.parent?.parent in Folder
}.findAll { WorkflowJob job ->
    job.name.startsWith('PR-')
}.each { WorkflowJob job ->
    job?.builds.each { WorkflowRun build ->
        if((new Date(build.startTimeInMillis)).before(thirtyDaysAgo)) {
            println "Deleting ${days} day ${build}"
            build.delete()
        }
    }
    if(!job?.builds) {
        println "Delete empty ${job}"
        job.delete()
    }
}

null
