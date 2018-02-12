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
   List all root projects by view and discover projects which are not
   associated with a view.
 */

import hudson.model.Job
import hudson.plugins.git.GitSCM
import hudson.scm.NullSCM
import hudson.scm.SCM
import jenkins.model.Jenkins
import org.jenkinsci.plugins.multiplescms.MultiSCM

void printSCM(Job j, SCM scm) {
    switch(scm) {
        case NullSCM:
            println "    ${j.displayName} has no SCM configured."
            break
        case GitSCM:
            println "    ${j.displayName} -> ${scm.repositories[0].URIs[0]}"
            break
        case MultiSCM:
            scm.configuredSCMs.each { s ->
                printSCM(j, s)
            }
            break
        default:
            throw new Exception("SCM class not supported: ${scm.class}")
            break
    }
}

j = Jenkins.instance

discovered_jobs = [].toSet()

Jenkins.instance.views.findAll { v ->
    v.displayName != j.primaryView.displayName
}.each { v ->
    println v.displayName
    v.items.each { j ->
        discovered_jobs << j
        printSCM(j, j.scm)
    }
}

println "Jobs under primary view '${j.primaryView.displayName}' not covered by other views"
((j.primaryView.items as Set) - discovered_jobs).each { j ->
    printSCM(j, j.scm)
}

null
