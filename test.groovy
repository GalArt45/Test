try{
    currentBuild.result="SUCCESS"
    stage "Prepare"
    node ("master"){
        cleanWs()
        GitPath="/*"
        GitURL="https://github.com/GalArt45/Test"
        GitBranch="*/master"
        checkout([$class: 'GitSCM', branches: [[name: GitBranch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'GalArt45', url: GitURL]]])
        stash excludes: '*.groovy', name: 'git'
        cleanWs()
    }
    node("centos") {
        cleanWs()
        unstash name: 'git'
        sh "ls -la"
        withCredentials([usernamePassword(credentialsId: 'vault', passwordVariable: 'VAULT_PASS', usernameVariable: 'user')]) {
            stage "Build App"
            ansiblePlaybook(
                    colorized: true,
                    playbook: 'test.yml',
                    inventory: 'hosts/hosts',
                    extras: '--vault-password-file=/home/GalArt/vault',
                    tags: 'build_app,debug',
                    extraVars: [
                            SP_VERSION : currentBuild.number
                    ]
            )
            stage "Deploy App"
            ansiblePlaybook(
                    colorized: true,
                    playbook: 'test.yml',
                    inventory: 'hosts/hosts',
                    extras: '--vault-password-file=/home/GalArt/vault',
                    tags: 'deploy_app,debug',
                    extraVars: [
                            SP_VERSION : currentBuild.number,
                            DB_REDEPLOY: DB_REDEPLOY
                    ]
            )
        }
        stage "Smoke"
        SmokeDoc(postgresql)
        SmokeDoc(tomcat)

        cleanWs()
    }
}
catch(err){
    println err
    currentBuild.result="FAILURE"
}
finally {
    println 'finish '+currentBuild.result
}

def SmokeDoc(container_name){
    String result = ""
    try {
        result = sh returnStdout: true, script: "docker ps -f name=${container_name} --format \"{{.ID}}\\t{{.Status}}\" -f status=running"
    }
    catch (err){
        println "Ошибка при проверке статуса контейнера ${container_name}"
        result = -1
        return result
    }

    if (result==~/[\s\S]*Up([\s\S]*?)$/){
        println "Контейнер ${container_name} запущен."
    }
    else{
        println "Контейнер ${container_name} не запущен!!!"
        currentBuild.result="FAILURE"
    }
    return result
}