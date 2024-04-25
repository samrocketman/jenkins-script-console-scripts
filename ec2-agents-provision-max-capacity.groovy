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
   When given an agent label associated with an EC2 plugin template, this
   script will provision the EC2 template to max capacity.

   WARNING this script was tested when provisioning limits were defined for the
   EC2 plugin with only 1 template.  Multiple templates and labels were not
   tested.  This was also not tested without EC2 limits set.  This was tested
   in a very simple environment where agent labels for EC2 instances did not
   conflict with any other Jenkins agent labels.

   WARNING THIS SCRIPT MAY COST YOU REAL MONEY BECAUSE YOUR AWS CAPACITY MAY
   NOT BE FREE.  I AM NOT RESPONSIBLE AND YOU HAVE BEEN WARNED.

   ec2 plugin 1.41
 */

import hudson.model.Label
import hudson.model.Node
import hudson.model.labels.LabelAtom
import hudson.plugins.ec2.AmazonEC2Cloud
import hudson.plugins.ec2.EC2AbstractSlave
import hudson.plugins.ec2.SlaveTemplate
import jenkins.model.Jenkins

// provision max EC2 instances for agent label
String agent_label = 'aws-agent'

Label agent = (Jenkins.instance.getLabel(agent_label)) ?: (new LabelAtom(agent_label))
Jenkins.instance.clouds.findAll { cloud ->
    int current_agents = agent.nodes.findAll { node ->
        node in EC2AbstractSlave &&
        node.cloud.is(cloud)
    }.size()

    (cloud in AmazonEC2Cloud) &&
    cloud.canProvision(agent) &&
    (cloud.getTemplate(agent).instanceCap - current_agents) > 0
}.each { AmazonEC2Cloud cloud ->
    SlaveTemplate t = cloud.getTemplate(agent)
    int desired_agents = t.instanceCap - agent.nodes.findAll {
        it in EC2AbstractSlave &&
        it.cloud.is(cloud)
    }.size()
    List<Node> nodes = t.provision(desired_agents, EnumSet.of(SlaveTemplate.ProvisionOptions.FORCE_CREATE))
    nodes.each { node ->
        Jenkins.instance.addNode(node)
    }
    println "Provisioned ${nodes.size()} new agents."
} ?: println('No agents provisioned.')

null
