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
