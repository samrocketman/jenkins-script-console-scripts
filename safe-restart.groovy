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
   This script starts a background thread which will wait for Jenkins to finish
   executing jobs before restarting.  The thread will abort if shutdown mode is
   disabled before jobs finish.

   Tested on Jenkins ver. 2.7.1
*/

import hudson.model.RestartListener
import java.util.logging.Level
import java.util.logging.Logger

//user configurable variable
if(!binding.hasVariable('timeout_seconds')) {
    timeout_seconds = 60
}

if(timeout_seconds in String) {
    timeout_seconds = Integer.decode(timeout_seconds)
}

//type check user defined parameters/bindings
if(!(timeout_seconds in Integer)) {
    throw new Exception('PARAMETER ERROR: timeout_seconds must be an integer.')
}

Logger logger = Logger.getLogger('jenkins.instance.restart')

//start a background thread
def thread = Thread.start {
    logger.log(Level.INFO, "Jenkins safe restart initiated.")
    while(true) {
        if(Jenkins.instance.isQuietingDown()) {
            if(RestartListener.isAllReady()) {
                Jenkins.instance.restart()
            }
            logger.log(Level.INFO, "Jenkins jobs are not idle.  Waiting ${timeout_seconds} seconds before next restart attempt.")
            sleep(timeout_seconds*1000)
        }
        else {
            logger.log(Level.INFO, "Shutdown mode not enabled.  Jenkins restart aborted.")
            break
        }
    }
}

println 'A safe restart has been scheduled.  See the Jenkins logs for restart status updates.  Logger is jenkins.instance.restart.'
