/*
    Copyright (c) 2015-2017 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   This reveals in Jenkins which projects are using a Jenkins agent with a
   particular label.  Useful for finding all projects using a particular set of
   agents.
*/
import hudson.model.Job

List labels = ['language:shell', 'language:ruby']

projects = [] as Set
//getAllItems searches a global lookup table of items regardless of folder structure
Jenkins.instance.getAllItems(Job.class).each { i ->
    Boolean labelFound = false
    labels.each { label ->
        if(i.class.simpleName == 'FreeStyleProject') {
            if(i.getAssignedLabelString().contains(label)) {
                labelFound = true
            }
        }
        else if(i.class.simpleName == 'WorkflowJob') {
            if(i.getDefinition().getScript().contains(label)) {
                labelFound = true
            }
        }
    }
    if(labelFound) {
        projects << "${i.fullName.split('/')[0]}/${i.displayName.split(' ')[0]}"
    }
}
projects.each { println it }
//null so no result shows up
null
