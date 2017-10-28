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
   This script completely disables Jenkins CLI and ensures that it will remain
   disabled when Jenkins is restarted.

   - Installs disable-jenkins-cli.groovy script to JENKINS_HOME/init.groovy.d
   - Evaluates disable-jenkins-cli.groovy to patch the Jenkins runtime so no
     restart is required.
*/

import java.security.MessageDigest
import jenkins.model.Jenkins

//downloadFile and sha256sum copied from sandscape
//https://github.com/sandscape/sandscape/blob/master/scripts/functions.groovy
boolean downloadFile(String url, String fullpath) {
    try {
        new File(fullpath).with { file ->
            //make parent directories if they don't exist
            if(!file.getParentFile().exists()) {
                file.getParentFile().mkdirs()
            }
            file.newOutputStream().with { file_os ->
                file_os << new URL(url).openStream()
                file_os.close()
            }
        }
    }
    catch(Exception e) {
        sandscapeErrorLogger(ExceptionUtils.getStackTrace(e))
        return false
    }
    return true
}

//can take a String or File as an argument
String sha256sum(def input) {
    MessageDigest.getInstance('SHA-256').digest(input.bytes).encodeHex().toString()
}

//main method
String remote_jenkins_cli_script = 'https://raw.githubusercontent.com/samrocketman/jenkins-script-console-scripts/master/disable-jenkins-cli.groovy'
String local_jenkins_cli_script = "${Jenkins.instance.root}/init.groovy.d/disable-jenkins-cli.groovy"
downloadFile(remote_jenkins_cli_script, local_jenkins_cli_script)
new File(local_jenkins_cli_script).with { f ->
    if(sha256sum(f) == '06defb6916c7b481bb48a34e96a2752de6bffc52e10990dce82be74076e037a4') {
        println "Disable Jenkins CLI script successfully installed to ${local_jenkins_cli_script} (patch persists on Jenkins restart)."
        try {
            evaluate(f)
            println 'Runtime has been patched to disable Jenkins CLI.  No restart necessary.'
        } catch(Exception e) {
            println "ERROR: Runtime patching has failed.  Removed ${local_jenkins_cli_script}"
            f.delete()
            throw e
        }
    } else {
        println 'ERROR: Disable Jenkins CLI script checksum mismatch; Aborting.'
        f.delete()
    }
}
null
