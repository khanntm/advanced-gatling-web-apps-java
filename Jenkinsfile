// Jenkins pipeline for the acetoy Gatling load test.
// Run it via "Build with Parameters" — pick a profile, target URL, and duration.
pipeline {
    agent any

    parameters {
        choice(
            name: 'TEST_TYPE',
            choices: ['INSTANT_USERS', 'RAMP_USERS', 'COMPLEX_INJECTION', 'CLOSED_MODEL'],
            description: 'Gatling injection profile (see acetoy.simulation.TestPopulation)'
        )
        string(
            name: 'BASE_URL',
            defaultValue: 'https://acetoys.uk',
            description: 'Target base URL under test'
        )
        string(
            name: 'TEST_DURATION',
            defaultValue: '60',
            description: 'Scenario loop duration in seconds'
        )
    }

    options {
        timeout(time: 60, unit: 'MINUTES')   // safety net so a stuck run never hangs forever
        timestamps()                         // prefix every console line with a timestamp
        disableConcurrentBuilds()            // never run two load tests against the target at once
    }

    stages {
        stage('Compile') {
            steps {
                sh './mvnw -B clean test-compile'
            }
        }

        stage('Load Test') {
            // Single quotes: the shell (not Groovy) expands the params, which Jenkins
            // exposes as environment variables. Safer than Groovy string interpolation.
            steps {
                sh '''
                    ./mvnw -B gatling:test \
                      -Dgatling.simulationClass=acetoy.AcetoySimulation \
                      -DTEST_TYPE="${TEST_TYPE}" \
                      -DbaseUrl="${BASE_URL}" \
                      -DTEST_DURATION="${TEST_DURATION}"
                '''
            }
        }
    }

    post {
        always {
            // Publishes the Gatling HTML report (and trend graphs across builds).
            // Requires the "Gatling" Jenkins plugin.
            gatlingArchive()
        }
    }
}
