//
// Custom step: Update build name and build description with relevant build data
//
def call() {
	def primeVersion = sh(script: "./gradlew -Preckon.scope=${env.designatedReckonScope} -Preckon.stage=${env.designatedReckonStage} -Pdemo4echo.designatedTagName=${params.DESIGNATED_VERSION} printApplicableVersion | grep Prime | awk '{print \$3}'", returnStdout: true)
	def adjustedBuildVersion = primeVersion ? "|v${primeVersion}" : ""
	def k8sJenkinsSlaveNodeName = sh(script: 'echo $NODE_HOST_NAME_ENV_VAR', returnStdout: true)

	buildName "#${env.BUILD_NUMBER}${adjustedBuildVersion}"
	buildDescription "${JOB_NAME}@${k8sJenkinsSlaveNodeName}"
}
