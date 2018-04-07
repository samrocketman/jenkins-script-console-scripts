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


   FreeStyle job named "_jervis_generator".
 */

import hudson.model.Job
import jenkins.model.Jenkins
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

//will delete disabled branches over all multibranch pipeline jobs in Jenkins
boolean cleanupAllJobs = false
//ignored if cleaning up all jobs
String jobFullName = 'samrocketman/jervis-example-project'
//removes disabled (closed) pull requests as well
boolean includePullRequests = false
//pretend to delete but don't actually delete, useful to see what would be deleted without modifying Jenkins
boolean dryRun = true

boolean isPullRequest(Job job) {
	BranchJobProperty prop
	if(job) {
		prop = job.getProperty(BranchJobProperty)
	}
	//check if the job is a pull request
	job && prop.branch && prop.branch.head && (prop.branch.head in PullRequestSCMHead)
}

void deleteDisabledJobs(WorkflowMultiBranchProject project, boolean includePullRequests = false, boolean dryRun = true) {
	project.items.findAll { Job j ->
		j.disabled && (includePullRequests || !isPullRequest(j))
	}.each { Job j ->
		println "${(dryRun)? 'DRYRUN: ' : ''}Deleted ${project.fullName} ${isPullRequest(j)? 'pull request' : 'branch'} ${j.name}"
		if(!dryRun) {
			j.delete()
		}
	}
}

if(dryRun) {
	println 'NOTE: DRYRUN mode does not make any modifications to Jenkins.'
}

if(cleanupAllJobs) {
	println "NOTE: iterating across all multibranch pipelines in Jenkins to clean up branches${(includePullRequests)? ' and pull requests' : ''}."
	Jenkins.instance.getAllItems(WorkflowMultiBranchProject.class).each { WorkflowMultiBranchProject project ->
		deleteDisabledJobs(project, includePullRequests, dryRun)
	}
}
else {
	println "NOTE: attempting to clean up specific job ${jobFullName} to clean up branches${(includePullRequests)? ' and pull requests' : ''}."
	if(jobFullName) {
		def project = Jenkins.instance.getItemByFullName(jobFullName)
		if(!project || !(project in WorkflowMultiBranchProject)) {
			throw new RuntimeException('ERROR: Job is not a multibranch pipeline project.  This script only works on multibranch pipelines.')
		}
		deleteDisabledJobs(project, includePullRequests, dryRun)
	}
	else {
		throw new RuntimeException('ERROR: Job full name not specified.  There is nothing to clean up so this is an error.')
	}
}

null
