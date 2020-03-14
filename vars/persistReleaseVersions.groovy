//
// Custom step: Update a file (in the repositiry) holding a summary of all the micro-servics latest versions and mark it with a proper tag
// Parameters:
//		releaseVersionsDataAsYamlStr:	The data (as a yaml string) to be saved in the designated file
//			default:							N/A
//
def call(String releaseVersionsDataAsYamlStr) {
	// Persist (and update) the yaml file into the root of the repository
	writeYaml file: "/root/.helm/${pipelineCommon.CONST_RELEASE_VERSIONS_FILE_NAME}", data: releaseVersionsDataAsYamlStr, overwrite: true
}
