import hudson.model.Result
import hudson.model.ParametersAction

Jenkins.instance.getItem('_jervis_generator').builds.findAll {
  //it.number > 715 &&
  it.result == Result.FAILURE
}.each {
  println "${it.getAction(ParametersAction).parameters.first().value}\n    ${it.absoluteUrl}"
}

null
