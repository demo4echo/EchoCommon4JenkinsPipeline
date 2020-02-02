//
// Determine the namespace the micro service is running in (currently the Jenkins Slave Pod is running in the default namespace)
//
def resolveNamespaceByBranchName() {
	node {
		println "Within resolveNamespaceByBranchName() => Node name is: [${env.NODE_NAME}]"

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

		println "Within assimilateEnvironmentVariables() => Node name is: [${env.NODE_NAME}]"

		def props = readProperties interpolate: true, file: 'EnvFile.properties'
		props.each {
			key,value -> env."${key}" = "${value}" 
		}
		
		println "JENKINS_SLAVE_K8S_DEPLOYMENT_CLOUD_NAME value is: [${env.JENKINS_SLAVE_K8S_DEPLOYMENT_CLOUD_NAME}]"
		
		// Manifest common sub module folder name
		def commonSubModuleFolderName = locateCommonSubModuleFolderName()
		env.COMMON_SUB_MODULE_FOLDER_NAME_ENV_VAR = commonSubModuleFolderName
		println "COMMON_SUB_MODULE_FOLDER_NAME_ENV_VAR value is: [${env.COMMON_SUB_MODULE_FOLDER_NAME_ENV_VAR}]"

		return env.JENKINS_SLAVE_K8S_DEPLOYMENT_CLOUD_NAME
//	}
}

//
// Locate sub module folder name
//
def locateCommonSubModuleFolderName() {
	def final COMMON_SUB_MODULE_MARKER_FILE_NAME = "_CommonSubModulePickup.markup"
	def commonSubModuleName
	def baseDir = new File('.')

	// Traverse the sub folders of the current folder
	baseDir.eachDir {
		def targetFilePath = "." + File.separator + it.name + File.separator + COMMON_SUB_MODULE_MARKER_FILE_NAME
		def currentFile = new File(targetFilePath)
		
		if (currentFile.exists() == true) {
			commonSubModuleName = it.name 
		}
	}

	return commonSubModuleName	
}

return this
