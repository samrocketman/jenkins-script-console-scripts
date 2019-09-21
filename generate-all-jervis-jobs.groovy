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
