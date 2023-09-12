@Library('viollier') _
import be.cegeka.jenkins.*

pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    agent {
        node { label 'java11chromium' }
    }
    triggers {
        cron('0 22 * * *')
    }
    parameters {
        choice(name: 'env', choices: ['DEV', 'TST', 'QAS'], description: 'Pick your env')
        choice(name: 'testSet', choices: ['Smoketests', 'Regression', 'FullSuite', 'Custom'], description: 'Pick your test set')
        string(name: 'testExec', defaultValue: '', description: 'Jira test execution key to report to? (optional)')
    }
    stages {
        stage('Testing') {
            steps {
                script {
                    truststoreSetup.setup()
                    sh "./gradlew clean test -Denvironment=${params.env} -PtestSet=${params.testSet} -DtestExec=${params.testExec}"
                }
            }
        }
    }
    post {
        always {
            step([$class: 'Publisher', reportFilenamePattern: '**/testng-results.xml'])
        }
    }
}