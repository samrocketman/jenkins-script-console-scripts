# Jenkins Script Console scripts

This repository contains [Jenkins Script Console][sc] scripts which are useful
for myself.

I've found the best way to call script console scripts with curl is by taking
advantage of bash `Command Substitution` (see `man bash`).  Here's an example
curl command.

    curl --data-urlencode "script=$(<./cleanup-offline-slaves.groovy)" http://localhost:8080/scriptText

By calling the script this way curl commands can be brief and the entire script
can be stored in a groovy file.

If I wanted to include authentication then I would make use of the `curl --user`
option.

    curl --user 'sam:password' --data-urlencode "script=$(<./cleanup-offline-slaves.groovy)" http://localhost:8080/scriptText

[sc]: https://wiki.jenkins-ci.org/display/JENKINS/Jenkins+Script+Console
