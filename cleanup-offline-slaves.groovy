println "Cleaning up offline slaves..."
hudson.model.Hudson.instance.slaves.each {
  if(it.getComputer().isOffline()) {
    println "Deleting ${it.name}"
    it.getComputer().doDoDelete()
  }
}
println "Done."
