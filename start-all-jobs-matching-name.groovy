  Jenkins.instance.getAllItems(AbstractItem.class).findAll {
    if (it.name.endsWith('-cleanup')) {
      return it.name
    }
  }.each { it.scheduleBuild(0, new Cause.UserIdCause()) }
