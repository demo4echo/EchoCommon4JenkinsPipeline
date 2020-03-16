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
	writeYaml file: pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME, data: releaseVersionsDataAsYamlStr, overwrite: true
//	writeYaml file: "${env.WORKSPACE}/${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME}", data: releaseVersionsDataAsYamlStr, overwrite: true

	// Upload to remote repository (including a matching tag)
//	push2RemoteWithGrgit()
	push2RemoteWithGit()
}

//
// Push the generated artifacts to the remote repository using git
//
def push2RemoteWithGit() {
	def GIT_ASKPASS_HELPER_FILE_NAME='./git-askpass.sh'
	def (buildUserId,buildUserName) = getBuildUserInfo()

	echo "Build user name is: [${buildUserName}]"
	echo "Build user id is: [${buildUserId}]"

	// Setup
	env.GIT_AUTHOR_NAME = buildUserName
	env.GIT_AUTHOR_EMAIL = "${buildUserId}@efrat.com"
	env.GIT_ASKPASS = GIT_ASKPASS_HELPER_FILE_NAME
	env.EMAIL = "${buildUserId}@efrat.com"
	env.GIT_COMMITTER_NAME = buildUserId
	env.GIT_COMMITTER_EMAIL = "${buildUserId}@efrat.com"

	// Verify
//	sh 'printenv'

	// Stage changes
	sh "git add ${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME}"

	// Commit changes
	sh "git commit -m 'Adding file ${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME}'"

	// Create a suitable tag to mark this update
	def (tagName,tagMessage) = manifestTagNameAndMessage()
	sh "git tag -a ${tagName} -m '${tagMessage}' -f"

	// Write the token helper temp file (will be deleted) and make it executable
	writeFile file: GIT_ASKPASS_HELPER_FILE_NAME, text: "echo ${env.GRGIT_USER}"
	sh "chmod +x ${GIT_ASKPASS_HELPER_FILE_NAME}"

	// Push to remote repo (including tags)
	sh "git push --all && git push --tags"
//	sh "git push --follow-tags"

	// Delete the temp token helper
	sh "rm -rf ${GIT_ASKPASS_HELPER_FILE_NAME}"
}

//
// Push the generated artifacts to the remote repository using Grgit - currently doesn't work!!!
//
def push2RemoteWithGrgit() {
	// Print some info
	def pwdDir = pwd()
	def userDir = System.properties['user.dir']
	echo "Workspace dir is: [${env.WORKSPACE}]"
	echo "Original pwd() dir is: [${pwdDir}]"
	echo "Original User dir: [${userDir}]"

	// Adjust current directory
	dir (env.WORKSPACE) {
		userDir = System.properties['user.dir']
		echo "Current User dir: [${userDir}]"

		System.properties['user.dir'] = env.WORKSPACE
		userDir = System.properties['user.dir']
		echo "Updated User dir: [${userDir}]"

		sh 'pwd'

		//
		// Work with the VCS (git) via the grgit library
		//

		env.GIT_DIR = env.WORKSPACE

		// Init repo
//		def grgit = Grgit.open(currentDir: env.WORKSPACE)
//		def grgit = Grgit.open(dir: env.WORKSPACE)
		def grgit = Grgit.open()

		echo "GIT_DIR is: [${env.GIT_DIR}]"

		// Stage changes
		grgit.add(patterns: [pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME])
//		grgit.add(patterns: ['.'])

		// Commit changes
		grgit.commit(message: "Updating ${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME} file", paths: [pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME])

		// Create a suitable tag to mark this update
		def (tagName,tagMessage) = manifestTagNameAndMessage()

		// Create the annotated tag (replace if needed)
		grgit.tag.add(name: tagName, message: tagMessage, force: true)

		// Setup authentication
		echo "The GRGIT token is: [${env.GRGIT_USER}]"
		System.properties.'org.ajoberstar.grgit.auth.username' = env.GRGIT_USER 

		// Push everything to the remote repo
		grgit.push(tags: true, remote: env.GIT_URL, force: true)
	}
}

//
// Resolves the applicable tag name and version and return them (name and message)
//
def manifestTagNameAndMessage() {
	def tagName,tagMessage

	// If we have a designated version - use it
	if (params.DESIGNATED_VERSION != null && params.DESIGNATED_VERSION.trim().isEmpty() == false) {
		tagName = params.DESIGNATED_VERSION
	}
	// Else fallback to the current date (can't use date-time (java.time.LocalDateTime().now()) since it contains ":" which isn't allowed in tag name)
	else {
		def currentDate = java.time.LocalDate.now()
		echo "No designated version observed, defaulting to current date: [${currentDate}]"
		tagName = currentDate
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

	// Return the results
	return [tagName,tagMessage]
}

//
// Obtaining the build user information (id and name)
//
@NonCPS
def getBuildUserInfo() {
	return [currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId(),currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserName()]
}
