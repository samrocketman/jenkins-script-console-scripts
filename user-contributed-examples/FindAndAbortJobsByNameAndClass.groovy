import hudson.*
import hudson.model.*
import jenkins.*
import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.*

// jobNameSearchParam should be absolute OR end in a slash. The result must be an instance of
def jobNameSearchParam = "RIPTiDE_SonarQube/SonarQube_PullRequest_Scanner/"
//def jobNameSearchParam = "RIPTiDE_SonarQube/SonarQube_PullRequest_Scanner/dashboard-launcher/Harish_Testing_branch"
def abortJobs = false
def classesToAbort = [
        org.jenkinsci.plugins.workflow.job.WorkflowJob
]

def itemsNotProcessed = [:]
def jobsFoundMatchingSearch = 0
def jobsFoundInClassesToAbort = 0
def jobsAborted = 0

println("************************************************** START ***************************************************************" +
        "\njobNameSearchParam: '${jobNameSearchParam}'" +
        "\nabortJobs: ${abortJobs}" +
        "\nclassesToAbort: ${classesToAbort}" +
        "\njobsFoundMatchingSearch: ${jobsFoundMatchingSearch}" +
        "\njobsFoundInClassesToAbort: ${jobsFoundInClassesToAbort}" +
        "\njobsAborted: ${jobsAborted}"
)
println("****************************************** Processing Started **********************************************************" +
        "\nSearching through AbstractItems...\n")

//JavaDoc: https://javadoc.jenkins-ci.org/hudson/model/AbstractItem.html
Jenkins.instance.getAllItems(AbstractItem.class).each { item ->
    if (item.fullName.contains(jobNameSearchParam)) {
        jobsFoundMatchingSearch++
        if (classesToAbort.contains(item.getClass())) {
            jobsFoundInClassesToAbort++
            WorkflowJob workflowJob = Jenkins.instance.getItemByFullName(item.fullName)
            println("Class: ${item.getClass()}\nfullName: ${item.fullName}")

            println("WorkflowJob RunMap.size(${workflowJob._getRuns().size()}):")
            //JavaDoc: https://javadoc.jenkins.io/plugin/workflow-job/org/jenkinsci/plugins/workflow/job/WorkflowRun.html
            workflowJob._getRuns().each { int runId, WorkflowRun build ->
                println("\t- runId: ${runId}" +
                        "\n\t  BuildStatusSumamry.message: ${build.getBuildStatusSummary().message}" +
                        "\n\t  Build.hasAllowKill(): ${build.hasAllowKill()}" +
                        "\n\t  Build.hasAllowTerm(): ${build.hasAllowTerm()}" +
                        "\n\t  Build.hasntStartedYet(): ${build.hasntStartedYet()}" +
                        "\n\t  Build.isBuilding(): ${build.isBuilding()}" +
                        "\n\t  Build.isInProgress(): ${build.isInProgress()}" +
                        "\n\t  Build.isLogUpdated(): ${build.isLogUpdated()}" +
                        "\n\t  build: ${build}\n")
                if (abortJobs && (build.isBuilding() || build.isInProgress())) {
                    build.finish(hudson.model.Result.ABORTED,
                            new IOException(
                                    "\nBuild Aborted by Jenkins Script Console" +
                                            "\nUser: ${User.current().getId()} (${User.current()})" +
                                            "\nBuild: ${build}" +
                                            "\nStack Trace:\n"
                            )
                    )
                    jobsAborted++
                }
            }

            // Add Newline between each WorkflowJob group.
            println("\n")
        }
    } else {
        def itemListForType = itemsNotProcessed.get("${item.getClass()}".toString(), [])
        itemListForType.add(item)
        itemsNotProcessed.put("${item.getClass()}".toString(), itemListForType)
    }
}

println("***************************************** Processing Complete **********************************************************" +
        "\njobNameSearchParam: '${jobNameSearchParam}'" +
        "\nabortJobs: ${abortJobs}" +
        "\nclassesToAbort: ${classesToAbort}" +
        "\njobsFoundMatchingSearch: ${jobsFoundMatchingSearch}" +
        "\njobsFoundInClassesToAbort: ${jobsFoundInClassesToAbort}" +
        "\njobsAborted: ${jobsAborted}"
)
println("\nitemsNotProcessed (wrong type or not matching jobNameSearchParam):")
itemsNotProcessed.each { itemType, itemListForType ->
    println("itemType: ${itemType}")
    itemListForType.each { item ->
        println("\t - ${item}")
    }
}

return "************************************************* END ******************************************************************"
