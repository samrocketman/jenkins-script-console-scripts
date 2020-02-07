/*
    Copyright (c) 2015-2020 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   Delete all Jenkins agents which are associated with a particular label.

   WARNING this script will permanently delete agents including all settings
   associated with an agent.  If the agent is associated with a Jenkins Cloud
   then INFRASTRUCTURE WILL ALSO LIKELY BE DELETED.
 */

import hudson.model.Label
import hudson.model.labels.LabelAtom
import jenkins.model.Jenkins

// delete all agents associated with label
String agent_label = 'some-agent'

Label agent = (Jenkins.instance.getLabel(agent_label)) ?: (new LabelAtom(agent_label))
agent.nodes*.computer.with {
    it*.doDoDelete()
    println "${it.size()} agents deleted."
}
