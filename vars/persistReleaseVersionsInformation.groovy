@Grab('org.ajoberstar.grgit:grgit-core:4.0.1') // version was 4.0.+
import org.ajoberstar.grgit.Grgit 

//
// Custom step: Update a file (in the repositiry) holding a summary of all the micro-servics latest versions and mark it with a proper tag
// Parameters:
//		releaseVersionsDataAsYamlStr:	The data (as a yaml string) to be saved in the designated file
//			default:							N/A
//
// Notes:
//	1. This step is meant to be used by Jenkinsfile4Release which means no Gradle support can be used!
// 2. It is assumed the calling party (e.g. the Jenkinsfile) has defined at this stage an environment variable named "GRGIT_USER"
//		which will be used for authentication by the grgit libraray (used below) during the push operation!
//
def call(String releaseVersionsDataAsYamlStr) {
	// Persist (and update) the yaml file into the root of the repository
	writeYaml file: "${env.WORKSPACE}/${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME}", data: releaseVersionsDataAsYamlStr, overwrite: true

	//
	// Work with the VCS (git) via the grgit library
	//

	// Init repo
	def grgit = Grgit.init(dir: pwd())

	// Stage changes
	grgit.add(patterns: [pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME])

	// Commit changes
	grgit.commit(message: "Updating ${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME} file")

	//
	// Create a suitable tag to mark this update
	//
	def tagName,tagMessage

	// If we have a designated version - use it
	if (params.DESIGNATED_VERSION != null && params.DESIGNATED_VERSION.trim().isEmpty() == false) {
		tagName = params.DESIGNATED_VERSION
	}
	// Else fallback to the current date-time
	else {
		def currentDateTime = java.time.LocalDateTime.now()
		echo "No designated version observed, defaulting to current date-time: [${currentDateTime}]"
		tagName = currentDateTime
	}

	// If we have a designated message - use it
	if (params.DESIGNATED_VERSION_MESSAGE != null && params.DESIGNATED_VERSION_MESSAGE.trim().isEmpty() == false) {
		tagMessage = params.DESIGNATED_VERSION_MESSAGE
	}
	// Else fallback to a pre-defined message
	else {
		def fallbackMessage = "Markup tag for ${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME} file"
		echo "No designated version message observed, defaulting to: [${fallbackMessage}]"
		tagMessage = fallbackMessage
	}

	// Create the annotated tag
	grgit.tag.add(name: tagName, message: tagMessage)

	// Push everything to the remote repo
	grgit.push(tags: true)
}
