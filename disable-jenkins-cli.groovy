/*
The MIT License

Copyright (c) 2015, Kohsuke Kawaguchi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
/*
   Disable Jenkins CLI.

   Copy script to $JENKINS_HOME/init.groovy.d/

   This init script for Jenkins fixes a zero day vulnerability.
   http://jenkins-ci.org/content/mitigating-unauthenticated-remote-code-execution-0-day-jenkins-cli
   https://github.com/jenkinsci-cert/SECURITY-218
   https://github.com/samrocketman/jenkins-script-console-scripts/blob/master/disable-jenkins-cli.groovy
 */

import jenkins.AgentProtocol
import jenkins.model.Jenkins
import hudson.model.RootAction

//determined if changes were made
configChanged = false

// disabled CLI access over TCP listener (separate port)
def p = AgentProtocol.all()
p.each { x ->
    if(x.name && x.name.contains("CLI")) {
        //println "remove ${x}"
        p.remove(x)
        configChanged = true
    }
}

// disable CLI access over /cli URL
def removal = { lst ->
    lst.each { x ->
        if(x.getClass().name.contains("CLIAction")) {
            //println "remove ${x}"
            lst.remove(x)
            configChanged = true
        }
    }
}
def j = Jenkins.instance;
removal(j.getExtensionList(RootAction.class))
removal(j.actions)

if(configChanged) {
    println 'Jenkins CLI has been disabled.'
} else {
    println 'Nothing changed. Jenkins CLI already disabled.'
}
