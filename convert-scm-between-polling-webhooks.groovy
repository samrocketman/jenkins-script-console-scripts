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
   This script iterates through all Jenkins jobs and finds only jobs that have
   SCM polling or GitHub webhooks triggers configured.  In each project, it
   will do the following:

   - Stop and remove the old trigger.
   - Create a new trigger depending on whether webhooks or polling will be
     used.
   - Start the trigger.  If GitHubPushTrigger, then will also register hook
     with GitHub.

   Note: This script requires the GitHub plugin to be configured with GitHub
   API token which has the scope admin:hook on projects.
 */
import com.cloudbees.jenkins.GitHubPushTrigger
import hudson.model.Job
import hudson.triggers.SCMTrigger
import hudson.triggers.Trigger

// user configurable settings
enableWebhooks = true
pollSpec = 'H/5 * * * *'
dryRun = true

List affected_jobs = Jenkins.instance.getAllItems(Job.class).findAll { Job job
    job.getTrigger((enableWebhooks) ? SCMTrigger.class : GitHubPushTrigger.class )
}.sort { it.fullName }

if(dryRun) {
    println([
        '[DRYRUN] would convert to using ',
        (enableWebhooks)? 'GitHub webhooks': 'SCM polling',
        ':\n    ',
        ((affected_jobs.collect { it.displayName }) ?: ['No jobs would change.']).join('\n    ')
    ].join())
}

((dryRun)? [] : affected_jobs).each { Job job ->
    Trigger trigger = job.getTrigger((enableWebhooks) ? SCMTrigger.class : GitHubPushTrigger.class)
    //stop and remove old trigger
    trigger.stop()
    job.removeTrigger(trigger.descriptor)
    //create a new trigger
    trigger = (enableWebhooks) ? new GitHubPushTrigger() : new SCMTrigger(pollSpec)
    job.addTrigger(trigger)
    //start the trigger (if GitHubPushTrigger, then webhooks are registered with GitHub project)
    trigger.start(job, true)
    println "Converted ${job.displayName} to use ${(enableWebhooks)? 'GitHub webhooks': 'SCM polling'}."
}
//discard script console result output
null
