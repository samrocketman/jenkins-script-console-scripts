/*
   Enable very aggressive cloud provisioning where over provisioning is not a
   concern (e.g. one shot executor clouds).

   Based on this write-up from CloudBees.
   https://support.cloudbees.com/hc/en-us/articles/115000060512-New-agents-are-not-being-provisioned-for-my-jobs-in-the-queue-when-I-have-agents-that-are-suspended
 */

System.setProperty('hudson.model.LoadStatistics.clock', '5000')
System.setProperty('hudson.model.LoadStatistics.decay', '0.5')
System.setProperty('hudson.agents.NodeProvisioner.MARGIN', '100')
System.setProperty('hudson.agents.NodeProvisioner.MARGIN0', '1.0')

println "Aggressive cloud provisioning configured."
