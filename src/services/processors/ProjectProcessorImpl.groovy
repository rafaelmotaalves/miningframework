package services.processors

import main.interfaces.ProjectProcessor
import java.util.regex.Pattern
import java.util.regex.Matcher

import static main.app.MiningFramework.arguments
import main.util.*
import main.project.*
import main.exception.*

class ProjectProcessorImpl implements ProjectProcessor {

    private GithubHelper githubHelper
    private TravisHelper travisHelper

    public ArrayList<Project> processProjects(ArrayList<Project> projects) {
        if (arguments.providedAccessKey()) {
            githubHelper = new GithubHelper(arguments.getAccessKey())
            travisHelper = new TravisHelper(arguments.getAccessKey())
            println "Processing projects"

            ArrayList<Project> projectsForks = new ArrayList<Project>()
            for (project in projects) {
                if (project.isRemote()) {
                    def forkedProject = githubHelper.fork(project)
                    String path = "${githubHelper.URL}/${forkedProject.full_name}"
                    Project projectFork = new Project(project.getName(), path)

                    projectsForks.add(projectFork)
                } else {
                    println "${project.getName()} is not remote and cant be forked"
                }
            }

            keepTryingToEnableTravisProjects(projectsForks, 10)

            return projectsForks
        } 
        return projects
    }

    private void keepTryingToEnableTravisProjects (ArrayList<Project> projects, int maxNumberOfTries) {
        /* This is a workaround to a limitation in travis api
        * You have to wait and sync multiple times to a project         
        * become available 
        */ 
        try {
            for (project in projects) {
                configureTravisProject(project)
            }
        } catch (TravisHelperException e) {
            travisHelper.syncAndWait()
            if (maxNumberOfTries > 0) {
                keepTryingToEnableTravisProjects(projects, maxNumberOfTries - 1)
            } else {
                throw new TravisHelperException("Number of sync tries exceeded")
            }
        }
    } 

    private void configureTravisProject (Project project) {
        String[] ownerAndName = project.getOwnerAndName()
        Map travisProject = travisHelper.getProject(ownerAndName[0], ownerAndName[1])
        travisHelper.enableTravis(travisProject.id)
        travisHelper.addEnvironmentVariable(travisProject.id, "GITHUB_TOKEN", arguments.getAccessKey())
    }
}
 