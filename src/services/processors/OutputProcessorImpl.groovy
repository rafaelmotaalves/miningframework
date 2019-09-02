
package services.processors

import main.interfaces.OutputProcessor

import static main.app.MiningFramework.arguments
import main.util.*
import main.project.*
import main.exception.*

class OutputProcessorImpl implements OutputProcessor {
    
    private final String SCRIPT_RUNNER = "python3"
    private final String SCRIPT_PATH = "./scripts/fetch_jars.py"

    public void processOutput() {
        if (arguments.providedAccessKey()) {
            println "Running fetch_jars script"

            String inputPath = arguments.getInputPath()
            String outputPath = arguments.getOutputPath()
            String token = arguments.getAccessKey()
            
            ProcessBuilder builder = ProcessRunner.buildProcess(".", SCRIPT_RUNNER, SCRIPT_PATH, inputPath, outputPath, token)
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    
            Process process = ProcessRunner.startProcess(builder)
            process.waitFor()
        }
        
    }

}