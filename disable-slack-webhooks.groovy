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
   Disable Slack outbound webhooks.

   This patches for potentially unknown security vulnerabilities in the Jenkins
   slack plugin outbound webhooks.  It makes sense to disable Slack outbound
   webhooks if your Jenkins instance is in a network in which slack.com can't
   reach.

   Copy script to $JENKINS_HOME/init.groovy.d/

   Source:
   https://github.com/samrocketman/jenkins-script-console-scripts/blob/master/disable-slack-webhooks.groovy
 */

import jenkins.model.Jenkins
import hudson.model.RootAction

def j = Jenkins.instance;
def removal = { lst ->
    lst.each { x ->
        if(x.getClass().name.contains("slack.webhook")) {
            //println "remove ${x}"
            lst.remove(x)
        }
    }
}
removal(j.getExtensionList(RootAction.class))
removal(j.actions)
