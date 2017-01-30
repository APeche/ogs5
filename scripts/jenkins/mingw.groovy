defaultCMakeOptions = '-DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=$CMAKE_TOOLCHAIN_FILE'

node('docker') {
    stage('Checkout') {
        dir('ogs') { checkout scm }
    }

    docker.image('ogs6/mingw-base').inside() {
        stage('Configure') {
            configure 'build', ''
        }

        stage('Build') {
            build 'build', ''
            if (env.BRANCH_NAME == 'master')
                build 'build', 'package'
        }
    }

    stage('Post') {
        archive 'build*/*.zip'
        if (env.BRANCH_NAME == 'master') {
            step([$class: 'S3BucketPublisher', dontWaitForConcurrentBuildCompletion: false, entries:
                [[bucket: 'opengeosys/ogs5-binaries/head', excludedFile: '', flatten: true,
                    gzipFiles: false, managedArtifacts: true, noUploadOnFailure: true,
                    selectedRegion: 'eu-central-1', sourceFile: 'build*/*.zip', storageClass:
                    'STANDARD', uploadFromSlave: false, useServerSideEncryption: false]],
                profileName: 'S3 UFZ', userMetadata: []])
        }
    }
}

def configure(buildDir, cmakeOptions) {
    sh "rm -rf ${buildDir} && mkdir ${buildDir}"
    sh "cd ${buildDir} && cmake ../ogs ${defaultCMakeOptions} ${cmakeOptions}"
}

def build(buildDir, target) {
    sh "cd ${buildDir} && make -j \$(nproc) ${target}"
}
