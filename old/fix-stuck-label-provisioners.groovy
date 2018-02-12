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
   Fix labels which are misreporting pending launches for clouds.  This occurs
   when a build gets stuck in the queue and there's no apparent reason it
   should be.  To debug this one can create a new logger for the following
   classes:

        com.github.kostyasha.yad.DockerProvisioningStrategy
        hudson.model.LoadStatistics$LoadStatisticsUpdater
        hudson.slaves.NodeProvisioner

 */

import hudson.model.Label
import hudson.slaves.NodeProvisioner.PlannedNode
import jenkins.model.Jenkins

Jenkins.instance.labels.each { Label label ->
    try {
        if(label.nodeProvisioner.getPendingLaunches().size() > 0) {
            println "STUCK LABEL: ${label}"
            label.@nodeProvisioner.@pendingLaunches.set([] as List<PlannedNode>)
            println "FIXED"
        }
    } catch(Exception e) {
        //label.@nodeProvisioner.@pendingLaunches.set([] as List<PlannedNode>)
        println "ERROR: ${label}"
    }
}
null
