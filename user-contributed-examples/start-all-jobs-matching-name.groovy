/**
    Author: https://github.com/deepy
    Warning: This code is unlicensed.
    See https://github.com/samrocketman/jenkins-script-console-scripts/blob/master/user-contributed-examples/README.md
  */

import hudson.model.Cause
import hudson.model.Job
import jenkins.model.Jenkins

Jenkins.instance.getAllItems(AbstractItem.class).findAll {
    it.name.endsWith('-cleanup')
}.each {
    it.scheduleBuild(0, new Cause.UserIdCause())
}
