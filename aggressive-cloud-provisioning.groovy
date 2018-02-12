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
   Enable very aggressive cloud provisioning where over provisioning is not a
   concern (e.g. one shot executor clouds).

   Based on this write-up from CloudBees.
   https://support.cloudbees.com/hc/en-us/articles/115000060512-New-agents-are-not-being-provisioned-for-my-jobs-in-the-queue-when-I-have-agents-that-are-suspended
 */

System.setProperty('hudson.model.LoadStatistics.clock', '5000')
System.setProperty('hudson.model.LoadStatistics.decay', '0.5')
System.setProperty('hudson.agents.NodeProvisioner.MARGIN', '100')
System.setProperty('hudson.agents.NodeProvisioner.MARGIN0', '1.0')

println "Aggressive cloud provisioning configured."
