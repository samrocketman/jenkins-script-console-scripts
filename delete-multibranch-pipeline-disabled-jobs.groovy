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
   Delete all abanded branhes and closed pull requests for multibranch pipeline jobs.

   Features:
     - Safest options defined by default to prevent accidental modification.
     - Dry run to see what would be deleted without modifying Jenkins.
     - Clean up all jobs in Jenkins or a specific job.
     - Optionally include deleting pull request jobs.
     - Can be a run from a Groovy job or from the Script console.
     - If run from a Groovy job, permissions are checked against the user
       building the job giving flexibility to expose this script in a self
       service manner.


   FreeStyle job named "_jervis_generator".
 */

import hudson.model.Cause.UserIdCause
import hudson.model.Item
import hudson.model.Job
import hudson.model.ParametersAction
import hudson.model.User
import hudson.security.AccessDeniedException2
import jenkins.model.Jenkins
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject


/*
   User configurable options.  If using a Groovy Job to execute this script,
   then don't bother customizing these values.  Instead, add the following
   parameters to the job.

   - cleanup_all_jobs (boolean parameter)
   - job_full_name (string parameter)
   - include_pull_requests (boolean parameter)
   - dry_run (boolean parameter)
 */


//will delete disabled branches over all multibranch pipeline jobs in Jenkins
boolean cleanupAllJobs = false
//ignored if cleaning up all jobs
String jobFullName = 'samrocketman/jervis-example-project'
//removes disabled (closed) pull requests as well
boolean includePullRequests = false
//pretend to delete but don't actually delete, useful to see what would be deleted without modifying Jenkins
boolean dryRun = true


/*
   Helper Functions
 */


boolean hasDeletePermission(Item item) {
    item.hasPermission(Item.DELETE)
}


void message(String message) {
    if(isGroovyJob) {
        out?.println message
    } else {
        println message
    }
}


boolean isPullRequest(Job job) {
    BranchJobProperty prop
    if(job) {
        prop = job.getProperty(BranchJobProperty)
    }
    //check if the job is a pull request
    job && (prop?.branch?.head in PullRequestSCMHead)
}


void deleteDisabledJobs(WorkflowMultiBranchProject project, boolean includePullRequests = false, boolean dryRun = true) {
    project.items.findAll { Job j ->
        j.disabled && (includePullRequests || !isPullRequest(j))
    }.each { Job j ->
        message "${(dryRun)? 'DRYRUN: ' : ''}Deleted ${project.fullName} ${isPullRequest(j)? 'pull request' : 'branch'} ${j.name} job."
        if(!dryRun) {
            j.delete()
        }
    }
}


def getJobParameter(String parameter, def defaultValue) {
    if(!isGroovyJob) {
        return defaultValue
    }
    def parameterValue = build?.actions.find {
        it in ParametersAction
    }?.parameters.find {
        it.name == parameter
    }?.value
    if((defaultValue in String) && (parameterValue in Boolean)) {
        'false' != parameterValue
    }
    else {
        parameterValue.asType(defaultValue.getClass())
    }
}


/*
   Main execution
 */


//bindings
isGroovyJob = !(false in ['build', 'launcher', 'listener', 'out'].collect { binding.hasVariable(it) })

if(isGroovyJob) {
    //authenticate as the user calling the build so appropriate permissions apply
    Jenkins.get().ACL.impersonate(User.get(build.getCause(UserIdCause.class).getUserId()).impersonate())

    //get parameters from the groovy job
    cleanupAllJobs = getJobParameter('cleanup_all_jobs', cleanupAllJobs)
    jobFullName = getJobParameter('job_full_name', jobFullName)
    includePullRequests = getJobParameter('include_pull_requests', includePullRequests)
    dryRun = getJobParameter('dry_run', dryRun)
}

if(dryRun) {
    message 'NOTE: DRYRUN mode does not make any modifications to Jenkins.'
}

if(cleanupAllJobs) {
    message "NOTE: iterating across all multibranch pipelines in Jenkins to clean up branches${(includePullRequests)? ' and pull requests' : ''}."
    Jenkins.get().getAllItems(WorkflowMultiBranchProject.class).findAll { WorkflowMultiBranchProject project ->
        hasDeletePermission(project)
    }.each { WorkflowMultiBranchProject project ->
        deleteDisabledJobs(project, includePullRequests, dryRun)
    }
}
else {
    message "NOTE: attempting to clean up specific job ${jobFullName} to clean up branches${(includePullRequests)? ' and pull requests' : ''}."
    if(jobFullName) {
        def project = Jenkins.get().getItemByFullName(jobFullName)
        if(!project || !(project in WorkflowMultiBranchProject)) {
            throw new RuntimeException('ERROR: Job is not a multibranch pipeline project.  This script only works on multibranch pipelines.')
        }
        if(!hasDeletePermission(project)) {
            throw new AccessDeniedException2(Jenkins.get().authentication, Item.DELETE)
        }
        deleteDisabledJobs(project, includePullRequests, dryRun)
    }
    else {
        throw new RuntimeException('ERROR: Job full name not specified.  There is nothing to clean up so this is an error.')
    }
}

null
