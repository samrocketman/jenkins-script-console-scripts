/*
    Copyright (c) 2015-2021 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   Find all builds performed within that past 24 hours whose console output
   contains the text listed in the search binding.
 */
import hudson.console.PlainTextConsoleOutputStream
import java.io.ByteArrayOutputStream
import java.util.Date
import jenkins.model.Jenkins
import org.apache.commons.io.IOUtils
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

if(binding.hasVariable('out') && binding.hasVariable('build')) {
    search = build.buildVariableResolver.resolve('search')
}
int HOURS_IN_MS = 3600000
Date oneDayAgo = new Date(System.currentTimeMillis() - (24 * HOURS_IN_MS))

println Jenkins.instance.getAllItems(WorkflowJob).findAll {
    WorkflowRun build = it.builds.first()
    (new Date(build.startTimeInMillis)).after(oneDayAgo)
}.collect {
    it.builds.findAll { WorkflowRun build ->
        (new Date(build.startTimeInMillis)).after(oneDayAgo)
    }
}.flatten().findAll { WorkflowRun build ->
    ByteArrayOutputStream os = new ByteArrayOutputStream()
    InputStreamReader is = build.logText.readAll()
    PlainTextConsoleOutputStream pos = new PlainTextConsoleOutputStream(os)
    IOUtils.copy(is, pos)
    String text = os.toString()
    Boolean result = text.contains(search)
    os.close()
    is.close()
    pos.close()
    result
}*.absoluteUrl.join('\n')
