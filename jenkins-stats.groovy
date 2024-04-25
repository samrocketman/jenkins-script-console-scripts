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
   This script generates high level Jenkins statistics for Jenkins instances
   using Jervis.
 */

import com.cloudbees.hudson.plugins.folder.Folder
import groovy.transform.Field
import hudson.model.Job
import hudson.model.User
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

import jenkins.plugins.git.GitTagSCMHead
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty


//globally scoped vars
j = Jenkins.instance
@Field HashMap count_by_type = [:]
count = j.getAllItems(Job.class).size()
jobs_with_builds = j.getAllItems(Job.class)*.getNextBuildNumber().findAll { it > 1 }.size()
global_total_builds = j.getAllItems(Job.class)*.getNextBuildNumber().sum { ((int) it) - 1 }
j.getAllItems().each { i ->
    count_by_type[i.class.simpleName.toString()] = (count_by_type[i.class.simpleName.toString()])? count_by_type[i.class.simpleName.toString()]+1 : 1
}

List known_users = User.getAll()*.id
organizations = j.getAllItems(Folder.class).findAll { !(it.name in known_users) }.size()
projects = j.getAllItems(WorkflowMultiBranchProject.class).size()
total_users = User.getAll().size()

total_pull_requests = j.getAllItems(WorkflowMultiBranchProject.class)*.getAllItems(Job.class).flatten().findAll {
    it.getProperty(BranchJobProperty)?.branch?.head in PullRequestSCMHead
}*.getNextBuildNumber().sum {
    ((int) it) - 1
}

total_tag_releases = j.getAllItems(WorkflowMultiBranchProject.class)*.getAllItems(Job.class).flatten().findAll {
    it.getProperty(BranchJobProperty)?.branch?.head in GitTagSCMHead
}*.getNextBuildNumber().sum {
    ((int) it) - 1
}

//display the information
println "Number of GitHub Organizations: ${organizations}"
println "Number of GitHub Projects: ${projects}"
println "Number of Jenkins jobs: ${count}"
println "Jobs with more than one build: ${jobs_with_builds}"
println "Number of users: ${total_users}"
println "Global total number of builds: ${global_total_builds}"
println "Global total number of pull requests executed: ${total_pull_requests}"
println "Global total number of tag releases executed: ${total_tag_releases}"
println "Count of projects by type."
count_by_type.each {
    println "  ${it.key}: ${it.value}"
}
//null because we don't want a return value in the Script Console
null
