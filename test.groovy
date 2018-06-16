try{
    currentBuild.result="SUCCESS"

    node("centos") {
        cleanWs()
        GitPath="/*"
        GitURL="https://github.com/GalArt45/Test"
        GitBranch="*/master"
        checkout([$class: 'GitSCM', branches: [[name: GitBranch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'GalArt45', url: GitURL]]])
        sh "ls -la"
        withCredentials([usernamePassword(credentialsId: 'vault', passwordVariable: 'VAULT_PASS', usernameVariable: 'user')]) {
            sh 'chmod +x print_vault.sh'
            echo "LS=${VAULT_PASS}"
            println "Замена конфигов."
            ansiblePlaybook (
                colorized: true,
                playbook: 'test.yml',
                inventory: 'hosts/hosts',
                extras: '--vault-password-file=print_vault.sh',
                tags: 'build_app',
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