try{
    node('master'){
        println "Hello"
        error '11'
        println 'her'
    }
}
catch(err){
    println err
}
finally {
    println 'finish '+currentBuild.result
}