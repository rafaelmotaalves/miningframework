package services.collectors

import main.interfaces.DataCollector
import main.project.*
import main.util.TravisHelper
import main.util.GithubHelper
import main.util.ProcessRunner
import main.exception.TravisHelperException
import main.util.FileManager
import static main.app.MiningFramework.arguments

import services.*

class BuildRequester implements DataCollector {

    static private final FILE_NAME = '.travis.yml'

    public void collectData(Project project, MergeCommit mergeCommit) {
        if (arguments.providedAccessKey()) {
            String branchName = mergeCommit.getSHA().take(5) + '_build_branch'
            
            checkoutCommitAndCreateBranch(project, branchName, mergeCommit.getSHA()).waitFor()
            File travisFile = new File("${project.getPath()}/.travis.yml")
            if (travisFile.delete()) {
                String[] ownerAndName = getRemoteProjectOwnerAndName(project)
                travisFile << getNewTravisFile(mergeCommit.getSHA(), ownerAndName[0], ownerAndName[1])
                commitChanges(project, "'Trigger build #${mergeCommit.getSHA()}'").waitFor()
                pushBranch(project, branchName).waitFor()
            }
            
            goBackToMaster(project).waitFor()
            println "${project.getName()} - Build requesting finished!"

        }
        
    }

    static private Process checkoutCommitAndCreateBranch(Project project, String branchName, String commitSha) {
        return ProcessRunner
            .runProcess(project.getPath(), 'git', 'checkout', '-b', branchName, commitSha)
    }

    static private String[] getRemoteProjectOwnerAndName(Project project) {
        String remoteUrl = ProcessRunner
            .runProcess(project.getPath(), 'git', 'config', '--get', 'remote.origin.url').getText()
        String[] splitedValues = remoteUrl.split('/')
        return [splitedValues[splitedValues.size() - 2], splitedValues[splitedValues.size() - 1]]
    }

    static private Process goBackToMaster(Project project) {
        return ProcessRunner.runProcess(project.getPath(), 'git', 'checkout', 'master')
    }

    static private Process pushBranch(Project project, String branchName) {
        return ProcessRunner.runProcess(project.getPath(), 'git', 'push','-u', 'origin', branchName)
    }

    static private Process commitChanges(Project project, String message) {
        return ProcessRunner.runProcess(project.getPath(), "git", "commit", "-a", "-m", "${message}")
    }

    static private String getRemoteUrl(Project project) {
        return ProcessRunner.
            runProcess(project.getPath(), "git", "config", "--get", "remote.origin.url").getText()
    }

    static private getNewTravisFile(String commitSha, String owner, String projectName) {
        String trimmedProjectName = projectName.replace('\n', '')
        return """
sudo: required
language: java

script:
  - mvn package -DskipTests

before_deploy:
    - mkdir build
    - find . -name '*.jar' -exec cp {} ./build \\;
    - cd /home/travis/build/${owner}/${trimmedProjectName}/build
    - tar -zcvf result.tar.gz *
deploy:
  provider: releases
  api_key:
    secure: \$GITHUB_TOKEN
  file: result.tar.gz
  name: fetchjar-${commitSha}
  file_glob: true
  overwrite: true
  skip_cleanup: true
  on:
    all_branches: true 
            """
    }

}