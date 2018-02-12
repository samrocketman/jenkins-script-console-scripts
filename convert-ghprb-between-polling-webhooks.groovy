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
   GHPRB triggers configured.  In each GHPRB configured job this script does
   the following:

   - Disables cron polling. (or enables it)
   - Enables managing webhooks for the repository. (or disables it)
   - Saves the job configuration.
   - Starts the trigger so webhooks are active in Jenkins and registered with
     the GitHub project via GitHub API.
 */

import hudson.model.Job
import hudson.triggers.Trigger
import java.lang.reflect.Field
import org.jenkinsci.plugins.ghprb.GhprbTrigger

//true - convert from polling to webhooks
//false - convert from webhooks to polling
pollToWebhooks = true
cron_field = 'H/15 * * * *'
dryRun = true

if(!('ghprb' in Jenkins.instance.pluginManager.plugins*.shortName)) {
    throw new Exception('GitHub Pull-Request Builder Plugin is not installed.  This script has aborted.')
}

Jenkins.instance.getAllItems(Job.class).findAll { Job j ->
    j.getTrigger(GhprbTrigger.class)
}.each { Job j ->
    if(!dryRun) {
        //for this job which has GHPRB enabled do the following:
        Trigger trigger = j.getTrigger(GhprbTrigger.class)
        //disable cron polling
        Field cron_field = trigger.getClass().getDeclaredField('cron')
        cron_field.setAccessible(true)
        cron_field.set(trigger, (pollToWebhooks)? '' : cron_field)
        //enable managing webhooks for the repository
        Field useGitHubHooks_field = trigger.getClass().getDeclaredField('useGitHubHooks')
        useGitHubHooks_field.setAccessible(true)
        useGitHubHooks_field.set(trigger, pollToWebhooks)
        //save the job configuration
        j.save()
        //start the trigger so webhooks are active in Jenkins and registered with the GitHub project via GitHub API
        trigger.start(j, false)
    }
    println "${(dryRun) ? 'DRYRUN: ' : ''}${j.name} converted."
}
//discard script console result output
null
