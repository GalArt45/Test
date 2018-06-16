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
            ansiblePlaybook (
                colorized: true,
                playbook: 'test.yml',
                inventory: 'hosts/hosts',
                extras: '--vault-password-file=/home/GalArt/vault',
                tags: 'build_app,debug',
                extraVars: [
                    SP_VERSION: currentBuild.number
                ]
            )
        }
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