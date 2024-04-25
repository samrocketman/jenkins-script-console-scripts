/*
    Copyright (c) 2015-2024 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   This script generates reads freestyle job output produced by
   jenkins-stats.groovy and generates a CSV output.  This is useful for loading
   into a calc program and generating graphs.
 */
List statsCsv = []

Jenkins.instance.getItem('__overall_metrics').builds.findAll {
    it.result == hudson.model.Result.SUCCESS
}.each {
    it.logText.readAll().text.tokenize('\n').findAll {
        it.contains(':') && !it.contains('Finished:') && !it.contains('/')
    }.collect {
        it.tokenize(':')*.trim()
    }.with {
        //println it
        if(!statsCsv) {
            statsCsv << it*.get(0).join(',')
        }
        statsCsv << it*.get(1).join(',')
    }
}
String header = statsCsv[0]
// print a CSV of all data to-date.
println((([header] + statsCsv[1..-1].reverse()).findAll { it }).join('\n'))
