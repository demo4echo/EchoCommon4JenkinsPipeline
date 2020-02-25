//
// Custom step:	Prevents from replayed builds to exexute,
// 					as otherwise such builds will generate artifacts (like docker images or helm charts)
//						with old/obsolete/stale code 
//
def call(String buildID) {
	def buildDisplayName = currentBuild.displayName
	def buildDecription = currentBuild.description

	echo "Current Display Name is: [${buildDisplayName}]"
	echo "Current Decription is: [${buildDecription}]"

	// Check if this is a replay of an old build and if so, stop the build
	if (buildDecription != null) {
		error "Build [${buildID}] is a replay of build [${buildDisplayName}] which is not allowed - build is stopped!"
	}
	else {
		echo "Build [${buildID}] is a new build - build is allowed to continue."
	}
}
