@Grab('org.ajoberstar.grgit:grgit-core:5.0.1') // version was 4.0.+
import org.ajoberstar.grgit.Grgit 

//
// Custom step: Update a file (in the repositiry) holding a summary of all the micro-servics latest versions and mark it with a proper tag
// Parameters:
//		releaseVersionsDataAsYamlStr:	The data (as a yaml string) to be saved in the designated file
//			default:							N/A
//
// Note: This step is meant to be used by Jenkinsfile4Release which means no Gradle support can be used!
//
def call(String releaseVersionsDataAsYamlStr) {
	// Persist (and update) the yaml file into the root of the repository
	writeYaml file: "${env.WORKSPACE}/${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME}", data: releaseVersionsDataAsYamlStr, overwrite: true

	// Work with grgit
	def grgit = Grgit.open(dir: pwd())

//	sleep 300
}
