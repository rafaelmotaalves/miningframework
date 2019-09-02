package main.app

@Grab('com.google.inject:guice:4.2.2')
import com.google.inject.*

import main.exception.InvalidArgsException
import main.exception.UnstagedChangesException
import main.exception.UnexpectedPostScriptException
import main.exception.NoAccessKeyException

import main.project.*
import main.arguments.*
import main.util.*

class Main {

    static main(args) {
        ArgsParser argsParser = new ArgsParser()
        try {
            Arguments appArguments = argsParser.parse(args)
            
            if (appArguments.isHelp()) {
                argsParser.printHelp()
            } else {
                Class injectorClass = appArguments.getInjector()
                Injector injector = Guice.createInjector(injectorClass.newInstance())
                MiningFramework framework = injector.getInstance(MiningFramework.class)

                framework.setArguments(appArguments)

                FileManager.createOutputFiles(appArguments.getOutputPath(), appArguments.isPushCommandActive())
            
                String inputPath = appArguments.getInputPath()
                ArrayList<Project> projectList = InputParser.getProjectList(inputPath)

                framework.setProjectList(projectList)
                framework.start()

                framework.runPostScript()
            }
    
        } catch (InvalidArgsException e) {
            println e.message
            println 'Run the miningframework with --help to see the possible arguments'
        } catch (UnstagedChangesException | UnexpectedPostScriptException | NoAccessKeyException e) {
            println e.message
        }
    }
}