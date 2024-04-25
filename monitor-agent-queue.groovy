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
   This Jenkins script console script will monitor Jenkins agents and the build
   queue.  It will notify slack when agents are offline and when jobs are stuck
   in the build queue for longer than the specified time threshold (2 hours).
 */

import hudson.model.Queue
import jenkins.model.Jenkins
import jenkins.plugins.slack.SlackNotifier
import jenkins.plugins.slack.StandardSlackService

//two hours in milliseconds; the value must be in milliseconds
long queueTimeAlertThreshold = 7200000

List offlineAgents = Jenkins.instance.slaves.findAll {
    it.computer.isOffline() && ! it.computer.isTemporarilyOffline()
}

List<String> message = ['Jenkins agents and build queue monitor service:']

if(offlineAgents) {
    println("Offline agents:\n    ${offlineAgents*.name.join('\n    ')}")
    message << "Number of agents offline: ${offlineAgents.size()}"
}

List longQueuedJobs = Queue.instance.buildableItems.findAll {
    System.currentTimeMillis() - it.inQueueSince > queueTimeAlertThreshold
}

if(longQueuedJobs) {
    message << "There are ${longQueuedJobs.size()} jobs waiting in the build queue for longer than ${queueTimeAlertThreshold} milliseconds."
}

if(message.size() > 1) {
    SlackNotifier.DescriptorImpl notifier = Jenkins.instance.getExtensionList('jenkins.plugins.slack.SlackNotifier$DescriptorImpl')[0]
    StandardSlackService publisher = new StandardSlackService(
            notifier.baseUrl,
            notifier.teamDomain,
            '',
            notifier.tokenCredentialId,
            notifier.botUser,
            notifier.room)
    message << "(<${build.getAbsoluteUrl()}console|Open monitoring job>)"
    publisher.publish(message.join('\n'), 'danger')
    println "Slack send message:\n    ${message.join('\n    ')}"
}
else {
    println 'All Green, trap is clean.'
}
