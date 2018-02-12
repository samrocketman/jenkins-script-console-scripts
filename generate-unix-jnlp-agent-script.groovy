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
   Generate a bash shell script meant to run on Unix-like systems.  The shell
   script will start a static JNLP agent.
 */

import groovy.text.SimpleTemplateEngine
import java.security.MessageDigest
import java.util.regex.Matcher
import java.util.regex.Pattern

if(!binding.hasVariable('agent_name')) {
    //binding doesn't exist so set a default
    agent_name = 'my-agent'
}
//type check user defined parameters/bindings
if(!(agent_name instanceof String)) {
    throw new Exception('PARAMETER ERROR: agent_name must be a string.')
}

//script bindings
jenkins = Jenkins.instance
node = jenkins.getNode(agent_name)
computer = node.computer
launcher = node.launcher

String checksum( def input ) {
    MessageDigest.getInstance('SHA-256').digest(input.bytes).encodeHex().toString()
}

String getJenkinsUrl() {
    jenkins.rootUrl.trim().replaceAll('/$', '') as String
}

shell_script_template = '''#!/bin/bash
#This shell script was automatically generated
#https://github.com/samrocketman/jenkins-script-console-scripts/generate-unix-jnlp-agent-script.groovy
#DESCRIPTION:
#  Start a JNLP agent

#USAGE:
#  Start the agent:
#    bash agent.sh start
#  Stop the agent:
#    bash agent.sh stop

JENKINS_URL="<%= jenkins_url %>"
JENKINS_HOME="<%= jenkins_home %>"
COMPUTER_URL="<%= computer_url %>"
COMPUTER_SECRET="<%= computer_secret %>"
JAVA_OPTS="<%= java_opts %>"
OS_KERNEL="$(uname)"
AGENT_PREFIX="${AGENT_PREFIX:-${JENKINS_HOME}}"
AGENT_PID="${AGENT_PREFIX}/agent.pid"
AGENT_LOG="${AGENT_PREFIX}/agent.log"

if [ "$1" = "stop" ]; then
  cd "${JENKINS_HOME}"
  if [ ! -f "${AGENT_PID}" ]; then
    echo "No ${AGENT_PID}.  Nothing to stop." 1>&2
    false
  else
    kill $(<"${AGENT_PID}")
    rm -f "${AGENT_PID}"
  fi
  exit $?
elif [ "$#" -gt 0 -a ! "$1" = "start" ]; then
  echo "Invalid argument: $0 [start|stop]" 1>&2
  echo "No argument assumes $0 start" 1>&2
  exit 1
fi
set -uxe

function download_url() {
  URL="$1"
  LOCAL_FILE="${1##*/}"
  if [ -x "$(type -p curl)" ]; then
    curl -f -o "${AGENT_PREFIX}/${LOCAL_FILE}" "${URL}"
  elif [ -x "$(type -p wget)" ]; then
    wget -O "${AGENT_PREFIX}/${LOCAL_FILE}" "${URL}"
  else
    echo "No supported download method found." 1>&2
    return 1
  fi
}

#pre-flight tests (if any fail then script will exit)
id -un
echo "JAVA_HOME=${JAVA_HOME}"
mkdir -p "${JENKINS_HOME}/logs"
cd "${JENKINS_HOME}"

#download JNLP slave.jar
JNLP_JAR_SHA="<%= jnlp_jar_sha %>"
download_url "${JENKINS_URL}/jnlpJars/slave.jar"
#verify integrity of download
if [ -x "$(type -P sha256sum)" ]; then
  echo "${JNLP_JAR_SHA}  slave.jar" | sha256sum -c -
elif [ -x "$(type -P shasum)" ]; then
  echo "${JNLP_JAR_SHA}  slave.jar" | shasum -a 256 -c -
fi

exec nohup "${JAVA_HOME}/bin/java" ${JAVA_OPTS} -jar slave.jar -jnlpUrl "${JENKINS_URL}/${COMPUTER_URL}/slave-agent.jnlp" -secret ${COMPUTER_SECRET} > "${AGENT_LOG}" 2>&1 &
echo $! > "${AGENT_PID}"'''.replaceAll(Pattern.quote('$'), Matcher.quoteReplacement('\\$'))


Map scriptBinding = [
    jenkins_url: getJenkinsUrl(),
    jenkins_home: node.remoteFS.trim(),
    computer_url: computer.url.trim().replaceAll('/$', '') as String,
    computer_secret: computer.jnlpMac.trim(),
    java_opts: (launcher.vmargs)?:"",
    jnlp_jar_sha: checksum("${getJenkinsUrl()}/jnlpJars/slave.jar".toURL())
]

println (new SimpleTemplateEngine().createTemplate(shell_script_template).make(scriptBinding))
null
