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
   This script is designed to iterate across all users in a Jenkins
   installation and automatically delete their account if they're disabled in
   LDAP.

   **Note:** Jenkins MUST be set up with a user search filter which determines
   via LDAP if an account is disabled.  The following is an example.

       (&(uid={0})(!(loginShell=/bin/false)))
*/

import hudson.model.User
import jenkins.model.Jenkins
import org.acegisecurity.userdetails.UsernameNotFoundException

User.all.each { u ->
	try {
		u.impersonate()
	} catch(UsernameNotFoundException e) {
		println "${u.id}: Deleting disabled account from ${Jenkins.instance.rootUrl}."
		u.delete()
	}
}
null
