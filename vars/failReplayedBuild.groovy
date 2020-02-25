//
// Custom step:	Prevents from replayed builds to exexute,
// 					as otherwise such builds will generate artifacts (like docker images or helm charts)
//						with old/obsolete/stale code 
//
def call(String buildID) {
	// https://stackoverflow.com/questions/51555910/how-to-know-inside-jenkinsfile-script-that-current-build-is-an-replay/52302879#52302879
	def isBuildReplay = currentBuild.rawBuild.getCauses().any {
		cause -> cause instanceof org.jenkinsci.plugins.workflow.cps.replay.ReplayCause
	}

	// Check if this is a replay of an old build and if so, stop the build
	if (isBuildReplay == true) {
		error "Build [${buildID}] is a replay of an old build, which is not allowed - build is stopped!"
	}
	else {
		echo "Build [${buildID}] is a new build - build is allowed to continue."
	}
}
