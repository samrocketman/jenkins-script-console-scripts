/*
    Copyright (c) 2015-2016 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   This script completely disables all update sites in Jenkins, invalidates plugin
   upgrade data, and deletes the cached update data.

   Additionally, it sets a system property to disable all update sites.
*/

import hudson.model.UpdateSite
import jenkins.model.Jenkins

def j = Jenkins.instance
for(UpdateSite site : j.getUpdateCenter().getSiteList()) {
    site.neverUpdate = true
    site.data = null
    site.dataLastReadFromFile = -1
    site.dataTimestamp = 0
    new File(j.getRootDir(), "updates/${site.id}.json").delete()
}

//https://wiki.jenkins-ci.org/display/JENKINS/Features+controlled+by+system+properties
System.setProperty('hudson.model.UpdateCenter.never', 'true')
