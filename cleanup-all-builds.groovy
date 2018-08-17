/*
    Copyright (c) 2018 Marco Davalos https://github.com/samrocketman/jenkins-script-console-scripts

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
    This script deletes all builds from all job of the Jenkins instance keeping the 
    last ${MAX_BUILDS} builds. It checks the build to delete is not building at that
    moment and it also keeps the builds that are:

        - keepLog (marked as "Keep this build forever")
        - lastStableBuild
        - lastSuccessfulBuild
        - lastSuccessfulBuild
        - lastUnstableBuild
        - lastUnsuccessfulBuild
*/

import jenkins.model.Jenkins
import hudson.model.Job

int MAX_BUILDS = 5 // max builds to keep

Jenkins.instance.getAllItems(Job.class).each { job ->

    job.builds.drop(MAX_BUILDS).findAll {

        !it.keepLog &&
        !it.building &&
        it != job.lastStableBuild &&
        it != job.lastSuccessfulBuild &&
        it != job.lastUnstableBuild &&
        it != job.lastUnsuccessfulBuild

    }.each { build ->
        build.delete()
    }
}
