//
// Custom step: Update build name and build description with relevant build data
//
def call() {
	def primeVersion = sh(script: "./gradlew -Preckon.scope=${env.JENKINS_SLAVE_K8S_RECKON_SCOPE} -Preckon.stage=${env.JENKINS_SLAVE_K8S_RECKON_STAGE} -Pdemo4echo.designatedTagName=${params.DESIGNATED_VERSION} printApplicableVersion | grep Prime | awk '{print \$3}'", returnStdout: true)
	def k8sJenkinsSlaveNodeName = sh(script: 'echo $NODE_HOST_NAME_ENV_VAR', returnStdout: true)
				
	buildName "#${env.BUILD_NUMBER}:v${primeVersion}"
	buildDescription "@${k8sJenkinsSlaveNodeName}"
}
