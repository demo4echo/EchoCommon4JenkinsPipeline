//
// Exported Constants
// Notes:
//		1. Don't use "def" in order to have these variables as global ones
//		2. All constants/variables must be with the "@groovy.transform.Field" annotation in order to be used within this script (since running inside a pipeline)
//

@groovy.transform.Field
K8S_AGENT_DEFAULT_CONTAINER='jdk-gradle-docker-k8s-helm'

@groovy.transform.Field
OPTIONS_BUILD_DISCARDER_LOG_ROTATOR_NUM_TO_KEEP_STR='25'

@groovy.transform.Field
PARAMS_TARGET_JENKINSFILE_FILE_NAME_OPTIONS=['Jenkinsfile','Jenkinsfile4Release']

@groovy.transform.Field
PARAMS_TARGET_RECKON_SCOPE_DEFAULT_VALUE='NA'

@groovy.transform.Field
PARAMS_TARGET_RECKON_SCOPE_OPTIONS=[PARAMS_TARGET_RECKON_SCOPE_DEFAULT_VALUE,'patch','minor','major']

@groovy.transform.Field
PARAMS_TARGET_RECKON_STAGE_DEFAULT_VALUE='NA'

@groovy.transform.Field
PARAMS_TARGET_RECKON_STAGE_OPTIONS=[PARAMS_TARGET_RECKON_STAGE_DEFAULT_VALUE,'dev','rc','final']

@groovy.transform.Field
CONST_JENKINS_SLAVE_POD_AGENT_BASE_LABEL='jenkins-slave-pod-agent'

@groovy.transform.Field
CONST_ENV_PROPERTIES_FILE_NAME='EnvFile.properties'

@groovy.transform.Field
CONST_COMMON_SUB_MODULE_PICKUP_MARKER_FILE_PATTERN='**/_CommonSubModulePickup.markup'

//
// Builds a proper Jenkins service pod agent label (name), taking into account the job name
//
def constructJenkinsSlavePodAgentLabel() {
	node {
		// Job name might contain "/" followed by the branch name, so we need to replace "/" with something acceptable (e.g. "_")
		def originalJobName = env.JOB_NAME
		def safeJobName = originalJobName.replace("/","_")
		
		return CONST_JENKINS_SLAVE_POD_AGENT_BASE_LABEL + "-${safeJobName}"
	}
}

//
// Determine the applicable k8s cloud (towards Jenkins' configuration of the K8S plugin) by the branch name
//
def resolveCloudNameByBranchName() {
	node {
//	node(env.NODE_NAME) {
//	node('master') {
		println "Within resolveCloudNameByBranchName() => Jenkins node name is: [${env.NODE_NAME}]"

		println "Branch name is: [${env.BRANCH_NAME}]"

		// Note: don't use ENV VARs here since they can't be read from their file at this stage!
		if (env.BRANCH_NAME == 'master') {
			env.CLOUD_NAME = 'production'
		} else if (env.BRANCH_NAME == 'integration') {                 
			env.CLOUD_NAME = 'staging'
		}
		else {
			env.CLOUD_NAME = 'development'		    
		}
		
		println "Resolved cloud name is: [${env.CLOUD_NAME}]"
		
		// Return the resolved cloud name
		return env.CLOUD_NAME
	}
}

//
// Determine the applicable k8s cloud (towards Jenkins' configuration of the K8S plugin) by the job name
//
def resolveCloudNameByJobName() {
	node {
		println "Within resolveCloudNameByJobName() => Jenkins node name is: [${env.NODE_NAME}]"

		// These variables are null here
		println "Branch name is: [${env.BRANCH_NAME}]"
		println "GIT branch is: [${env.GIT_BRANCH}]"

		// Work with Job name instead of Git branch name
		println "Job name is: [${env.JOB_NAME}]"
		def projectedBranchName = env.JOB_NAME.split(/-/).last()
		println "Projected branch name is: [" + projectedBranchName + "]"

		// Set the target cloud name
		env.CLOUD_NAME = projectedBranchName
		println "Resolved cloud name is: [${env.CLOUD_NAME}]"
		
		// Return the resolved cloud name
		return env.CLOUD_NAME
	}
}

//
// Determine the namespace the micro service is running in (currently the Jenkins Slave Pod is running in the default namespace)
//
def resolveNamespaceByBranchName() {
	node {
		println "Within resolveNamespaceByBranchName() => Jenkins node name is: [${env.NODE_NAME}]"

		println "Branch name is: [${env.BRANCH_NAME}]"
		println "Production branch name ENV_VAR is: [${env.PRODUCTION_BRANCH_NAME_ENV_VAR}]"
		println "Staging branch name ENV_VAR is: [${env.STAGING_BRANCH_NAME_ENV_VAR}]"

		// If we are on the production or staging branches return the regular name (e.g. demo4echo), else return the branch namne itself
		if (env.BRANCH_NAME == env.PRODUCTION_BRANCH_NAME_ENV_VAR || env.BRANCH_NAME == env.STAGING_BRANCH_NAME_ENV_VAR) {                 
			env.RESOLVED_NAMESPACE = env.SERVICE_NAME_ENV_VAR
		}
		else {
			env.RESOLVED_NAMESPACE = env.BRANCH_NAME
		}
		
		println "Resolved namespace is: [${env.RESOLVED_NAMESPACE}]"
		
		// Return the resolved namespsace
		return env.RESOLVED_NAMESPACE
	}
}

//
// Load all the properties in the per brnach designated file as environment variables
//
def assimilateEnvironmentVariables() {
//	node(env.NODE_NAME) {
//		checkout(scm) => don't need it as we'll call the function after the repository has been fetched (checkout(scm) is called in the 'agent' phase)

		println "Within assimilateEnvironmentVariables() => Jenkins node name is: [${env.NODE_NAME}]"

		// Load properties from file and turn into environment variables
		def selfProps = readProperties interpolate: true, file: CONST_ENV_PROPERTIES_FILE_NAME
		selfProps.each {
			key,value -> env."${key}" = "${value}" 
		}
		
		// Overwrite designated environment variables values if applicable values were passed as parameters
		// Note - this call must happen AFTER the environment variables were loaded from the file!
		assimilateParameters()

		// Show resolved environment variables values
		println "JENKINS_SLAVE_K8S_DEPLOYMENT_CLOUD_NAME value is: [${env.JENKINS_SLAVE_K8S_DEPLOYMENT_CLOUD_NAME}]"
		println "JENKINS_SLAVE_K8S_RECKON_SCOPE value is: [${env.JENKINS_SLAVE_K8S_RECKON_SCOPE}]"
		println "JENKINS_SLAVE_K8S_RECKON_STAGE value is: [${env.JENKINS_SLAVE_K8S_RECKON_STAGE}]"

		// Manifest common sub module folder name
		def commonSubModuleFolderName = locateCommonSubModuleFolderName()
		env.COMMON_SUB_MODULE_FOLDER_NAME_ENV_VAR = commonSubModuleFolderName
		println "COMMON_SUB_MODULE_FOLDER_NAME_ENV_VAR value is: [${env.COMMON_SUB_MODULE_FOLDER_NAME_ENV_VAR}]"

		return env.JENKINS_SLAVE_K8S_DEPLOYMENT_CLOUD_NAME
//	}
}

//
// Digest applicable parameters and overwrite matching environment variables if needed
//
def assimilateParameters() {
		println "Within assimilateParameters() => Jenkins node name is: [${env.NODE_NAME}]"

		// Overwrite the reckon scope and stage designated values if applicable values were passed as parameters
		if (params.TARGET_RECKON_SCOPE != PARAMS_TARGET_RECKON_SCOPE_DEFAULT_VALUE)
		{
			env.JENKINS_SLAVE_K8S_RECKON_SCOPE = params.TARGET_RECKON_SCOPE
		}
		if (params.TARGET_RECKON_STAGE != PARAMS_TARGET_RECKON_STAGE_DEFAULT_VALUE)
		{
			env.JENKINS_SLAVE_K8S_RECKON_STAGE = params.TARGET_RECKON_STAGE
		}
}

//
// Locate sub module folder name
//
def locateCommonSubModuleFolderName() {
	println "Within locateCommonSubModuleFolderName() => Jenkins node name is: [${env.NODE_NAME}]"

	def markupFiles = findFiles(glob: CONST_COMMON_SUB_MODULE_PICKUP_MARKER_FILE_PATTERN)
	def commonSubModuleMarkupFileRelativePath = markupFiles[0].path
	def (commonSubModuleFolderName,commonSubModulePickupFileName) = commonSubModuleMarkupFileRelativePath.tokenize('/')
	def commonSubModuleName = commonSubModuleFolderName

/**
	def baseDir = new File('.')

	// Traverse the sub folders of the current folder
	baseDir.eachDir {
		def targetFilePath = "." + File.separator + it.name + File.separator + COMMON_SUB_MODULE_MARKER_FILE_NAME
		def currentFile = new File(targetFilePath)
		
		if (currentFile.exists() == true) {
			commonSubModuleName = it.name
		}
	}
*/
	return commonSubModuleName
}

return this
