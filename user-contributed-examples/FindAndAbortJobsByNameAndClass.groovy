/**
    Author: https://github.com/ranma2913
    Warning: This code is unlicensed.
    See https://github.com/samrocketman/jenkins-script-console-scripts/blob/master/user-contributed-examples/README.md
  */


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

println("""
************************************************** START ***************************************************************" +
jobNameSearchParam: '${jobNameSearchParam}'
abortJobs: ${abortJobs}
classesToAbort: ${classesToAbort}
jobsFoundMatchingSearch: ${jobsFoundMatchingSearch}
jobsFoundInClassesToAbort: ${jobsFoundInClassesToAbort}
jobsAborted: ${jobsAborted}
""".trim())
println("""****************************************** Processing Started **********************************************************
Searching through AbstractItems...""")

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
                println("""|\t- runId: ${runId}
                        |\t  BuildStatusSumamry.message: ${build.getBuildStatusSummary().message}
                        |\t  Build.hasAllowKill(): ${build.hasAllowKill()}
                        |\t  Build.hasAllowTerm(): ${build.hasAllowTerm()}
                        |\t  Build.hasntStartedYet(): ${build.hasntStartedYet()}
                        |\t  Build.isBuilding(): ${build.isBuilding()}
                        |\t  Build.isInProgress(): ${build.isInProgress()}
                        |\t  Build.isLogUpdated(): ${build.isLogUpdated()}
                        |\t  build: ${build}""".stripMargin() + '\n')
                if (abortJobs && (build.isBuilding() || build.isInProgress())) {
                    build.finish(hudson.model.Result.ABORTED,
                            new IOException("""
                                    |Build Aborted by Jenkins Script Console
                                    |User: ${User.current().getId()} (${User.current()})
                                    |Build: ${build}
                                    |Stack Trace:""".stripMargin()
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

println("""
***************************************** Processing Complete **********************************************************" +
jobNameSearchParam: '${jobNameSearchParam}'
abortJobs: ${abortJobs}
classesToAbort: ${classesToAbort}
jobsFoundMatchingSearch: ${jobsFoundMatchingSearch}
jobsFoundInClassesToAbort: ${jobsFoundInClassesToAbort}
jobsAborted: ${jobsAborted}""")
println("\nitemsNotProcessed (wrong type or not matching jobNameSearchParam):")
itemsNotProcessed.each { itemType, itemListForType ->
    println("itemType: ${itemType}")
    itemListForType.each { item ->
        println("\t - ${item}")
    }
}

return "************************************************* END ******************************************************************"
