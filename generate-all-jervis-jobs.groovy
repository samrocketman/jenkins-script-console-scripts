/*
    Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
/**
  This script searches all jobs in a Jenkins instance and regenerates them by
  building the Jervis generator job with parameters.  Useful for migrating Job
  DSL script changes as Jenkins is upgraded or Jervis is changed.
  */
import hudson.model.ParametersAction
import hudson.model.StringParameterValue
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

def generator_job = Jenkins.instance.getItemByFullName('_jervis_generator')

Jenkins.instance.getAllItems(WorkflowMultiBranchProject).collect { project ->
    project.getSCMSources().find { source ->
        source in GitHubSCMSource
    }.getRepositoryUrl() -~ '^https://github.com/'
}.sort().unique().each { String project ->
    def actions = new ParametersAction([new StringParameterValue('project', project)])
    generator_job.scheduleBuild2(0, actions)
    println "Scheduled project ${project}."
}
