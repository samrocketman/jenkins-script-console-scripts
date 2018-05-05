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
/**
  Search all freestyle jobs in a Jenkins instance.  Find any job containing any
  one of the commands being used in a list of strings.
 */
import hudson.model.FreeStyleProject
import hudson.tasks.Shell
import jenkins.model.Jenkins

//find any of the following strings existing in a freestyle job in Jenkins
List<String> strings = ['git clone', 'echo']
//require a freestyle job to contain all of the strings in the list within shell steps?
Boolean containsAll = false

println Jenkins.instance.getAllItems(FreeStyleProject.class).findAll { job ->
    job.builders.findAll { it in Shell } &&
    job.builders.findAll {
        it in Shell
    }.collect { shell ->
        shell.command
    }.join('\n').with { String script ->
        Boolean found = ((!containsAll) in strings.collect { script.contains(it) })
        //XOR the found result
        found ^ containsAll
    }
}.collect { job ->
    job.absoluteUrl
}.join('\n')
