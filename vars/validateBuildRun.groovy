//
// Custom step: Verifies if the current build run should be allowed to run or not
// Parameters:
//		buildID:					The identifier of the current build
//			default:				env.BUILD_ID
//		allowReplayedBuild:	Indicating if a build replay is allowed or not
//			default:				false (logic - if allowed, such builds will generate artifacts (like docker images or helm charts)	with old/obsolete/stale code)
//
def call(String buildID = env.BUILD_ID,boolean allowReplayedBuild = false) {
	// https://stackoverflow.com/questions/51555910/how-to-know-inside-jenkinsfile-script-that-current-build-is-an-replay/52302879#52302879
	// https://javadoc.jenkins.io/plugin/workflow-cps/org/jenkinsci/plugins/workflow/cps/replay/ReplayCause.html
//	def isReplayCause = currentBuild.rawBuild.getCauses().any {
//		cause -> cause instanceof org.jenkinsci.plugins.workflow.cps.replay.ReplayCause
//	}
	def replayCause = currentBuild.rawBuild.getCauses().find {
		it instanceof org.jenkinsci.plugins.workflow.cps.replay.ReplayCause
	}

	// Check applicable conditions on build run
	if (allowReplayedBuild == false && replayCause != null) {
		error "Build [${buildID}] is a replay of build [#${replayCause.getOriginalNumber()}], which is not allowed - build is stopped!"
	}
	else {
		echo "Build [${buildID}] is a new build - build is allowed to continue."
	}
}
