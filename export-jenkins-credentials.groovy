/*
    Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

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
   This script does a credential export which is compatible with
   https://github.com/samrocketman/jenkins-bootstrap-shared/blob/master/scripts/credentials-multitype.groovy
 */
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.util.Secret
import jenkins.model.Jenkins

SystemCredentialsProvider creds_config = Jenkins.instance.getExtensionList(SystemCredentialsProvider).first()

List creds = creds_config.getDomainCredentialsMap().get(Domain.global())

def getCredential(def cred) {
    Map credential = [:]
    switch(cred.class.simpleName) {
        case 'AWSCredentialsImpl':
            credential = [
                credential_type: 'AWSCredentialsImpl',
                credentials_id: cred.id,
                description: cred.description ?: '',
                access_key: cred.accessKey ?: '',
                secret_key: cred.secretKey.plainText ?: '',
                iam_role_arn: cred.iamRoleArn ?: '',
                iam_mfa_serial_number: cred.iamMfaSerialNumber ?: '',
                scope: cred.scope.toString().toLowerCase()
            ]
            break
        case 'BasicSSHUserPrivateKey':
            credential = [
                credential_type: 'BasicSSHUserPrivateKey',
                credentials_id: cred.id,
                description: cred.description ?: '',
                user: cred.username ?: '',
                key_passwd: cred.passphrase?.plainText ?: '',
                key: ((cred.privateKey instanceof Secret) ? cred.privateKey.plainText : cred.privateKey) ?: '',
                scope: cred.scope.toString().toLowerCase()
            ]
            break
        case 'UsernamePasswordCredentialsImpl':
            credential = [
                credential_type: 'UsernamePasswordCredentialsImpl',
                credentials_id: cred.id,
                description: cred.description ?: '',
                user: cred.username ?: '',
                password: cred.password.plainText ?: '',
                scope: cred.scope.toString().toLowerCase()
            ]
            break
        case 'StringCredentialsImpl':
            credential = [
                credential_type: 'StringCredentialsImpl',
                credentials_id: cred.id,
                description: cred.description ?: '',
                secret: cred.secret.plainText ?: '',
                scope: cred.scope.toString().toLowerCase()
            ]
            break
        case 'GitHubAppCredentials':
            credential = [
                credential_type: 'GitHubAppCredentials',
                credentials_id: cred.id,
                description: cred.description ?: '',
                appid: cred.appID,
                apiuri: cred.apiUri,
                owner: cred.owner,
                key: ((cred.privateKey instanceof Secret) ? cred.privateKey.plainText : cred.privateKey) ?: ''
            ]
            break
        default:
            println "Missing support for '${cred.class.name}'."
    }
    credential
}

List credsMap = creds.collect {
    getCredential(it)
}.findAll { it }

// pretty print
println('[')
println('    ' + credsMap.collect { Map cred ->
    '[\n        ' +
    cred.collect { k, v ->
        "${k}: ${v.inspect()}"
    }.join(',\n        ') +
'\n    ]'
}.join(',\n    '))
println(']')
