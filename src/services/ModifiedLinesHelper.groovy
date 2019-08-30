package services

import main.interfaces.DataCollector
import java.util.regex.Pattern
import java.util.regex.Matcher

import static main.app.MiningFramework.arguments
import main.util.*
import main.project.*

class ModifiedLinesHelper {

    public Set<MutuallyModifiedMethod> getMutuallyModifiedAttributesAndMethods(Project project, MergeCommit mergeCommit) {
        Set <MutuallyModifiedMethod> mutuallyModifiedMethodsAndLines = new Set()
        
        Set<String> mutuallyModifiedFiles = getMutuallyModifiedFiles(project, mergeCommit)
        
        for(file in mutuallyModifiedFiles) {
            def mergeModifiedMethods = getModifiedMethodsAndAttributes(project, file, mergeCommit.getAncestorSHA(), mergeCommit.getSHA())
            
            def mutuallyModifiedMethods = getMutuallyModifiedMethodsAndAttributes(project, mergeCommit, file)
            
            if (mutuallyModifiedMethods.size() > 0) {

                for(methodDeclaration in mergeModifiedMethods) {
                    MutuallyModifiedMethod methodWithLines = getMutuallyModifiedMethodWithLines(mutuallyModifiedMethods, methodDeclaration)
                    
                    mutuallyModifiedMethodsAndLines.add(methodWithLines)
                }

            }
        }
        return mutuallyModifiedMethodsAndLines
    }
    
    private MutuallyModifiedMethod getMutuallyModifiedMethodWithLines(Map<String, ModifiedDeclaration[]> parentsModifiedDeclarations, ModifiedDeclaration mergeModifiedDeclaration) {
        ModifiedDeclaration[] mutuallyModifiedDeclarations = parentsModifiedDeclarations[mergeModifiedDeclaration.getSignature()]
        if (mutuallyModifiedDeclarations != null) {

            MutuallyModifiedMethod mutuallyModifiedMethod = new MutuallyModifiedMethod(mergeModifiedDeclaration.getSignature())
           
            ModifiedDeclaration leftModifiedDeclaration = mutuallyModifiedDeclarations[0]
            ModifiedDeclaration rightModifiedDeclaration = mutuallyModifiedDeclarations[1]

            for(line in mergeModifiedDeclaration.getModifiedLines()) {
                    
                if (containsLine(leftModifiedDeclaration, line)) {
                    if (line.getType() == Modification.ADDED || line.getType() == Modification.CHANGED) {
                        mutuallyModifiedMethod.addLeftAddedLine(line)
                    } else {
                        mutuallyModifiedMethod.addLeftDeletedLine(line)
                    }
                }

                if(containsLine(rightModifiedDeclaration, line)) {
                    if (line.getType() == Modification.ADDED || line.getType() == Modification.CHANGED) {
                        mutuallyModifiedMethod.addRightAddedLine(line)
                    } else {
                        mutuallyModifiedMethod.addRightDeletedLine(line)
                    }
                }    
            }

            return mutuallyModifiedMethod
        }
        return null
    }

    private Set<String> getMutuallyModifiedFiles(Project project, MergeCommit mergeCommit) {
        Set<String> leftModifiedFiles = FileManager.getModifiedFiles(project, mergeCommit.getLeftSHA(), mergeCommit.getAncestorSHA())
        Set<String> rightModifiedFiles = FileManager.getModifiedFiles(project, mergeCommit.getRightSHA(), mergeCommit.getAncestorSHA())
        Set<String> mutuallyModifiedFiles = new HashSet<String>(leftModifiedFiles)
        mutuallyModifiedFiles.retainAll(rightModifiedFiles)

        return mutuallyModifiedFiles
    }

    private Map<String, ModifiedDeclaration[]> getMutuallyModifiedMethodsAndAttributes(Project project, MergeCommit mergeCommit, String file) {
        Set<ModifiedLine> leftModifiedMethods = getModifiedMethodsAndAttributes(project, file, mergeCommit.getAncestorSHA(), mergeCommit.getLeftSHA())
        Set<ModifiedLine> rightModifiedMethods = getModifiedMethodsAndAttributes(project, file, mergeCommit.getAncestorSHA(), mergeCommit.getRightSHA())
        def mutuallyModifiedMethods = getMethodsIntersection(leftModifiedMethods, rightModifiedMethods)
       
        return mutuallyModifiedMethods;
    }
  
    private boolean containsLine(ModifiedDeclaration method, ModifiedLine line) {
        for(lineit in method.getModifiedLines())
            if(lineit.equals(line))
                return true
        return false
    }

    /*
        DiffJ's output for methods' modification is the following:
        <rangei>,<rangef>: code (changed | added | removed) in <methodname>

        - rangei is a range of lines affected from the 'initial' file.
        - rangef is a range of lines affected from the 'final' file.
        - changed is reported when a removal and addition happen to the same line.
        
        After that, Diffj outputs three formats of line:
        < content 
        < content1 } removed lines (size of rangei).
        < ...     
        --- -> separator
        > content 
        > content1 } added lines (size of rangef).
        > ...  
        
        This algorithm detects such lines, associating them with their correspondent modifications.
        Also, it counts the rangef to check lines number.
    */
    private Set<ModifiedDeclaration> getModifiedMethodsAndAttributes(Project project, String filePath, String ancestorSHA, String commitSHA) {
        Set<ModifiedDeclaration> modifiedDeclarations = new HashSet<ModifiedDeclaration>()
        File ancestorFile = FileManager.copyFile(project, filePath, ancestorSHA) 
        File mergeFile = FileManager.copyFile(project, filePath, commitSHA)

        Process diffJ = ProcessRunner.runProcess('dependencies', 'java', '-jar', 'diffj.jar', ancestorFile.getAbsolutePath(), mergeFile.getAbsolutePath())
        BufferedReader reader = new BufferedReader(new InputStreamReader(diffJ.getInputStream()))
        String[] output = reader.readLines() 

        def methodAnalysisExpression = ~/.+ code (changed|added|removed) in .+/
        for(int i = 0; i < output.length; i++) {
            String line = output[i]

            if(line ==~ methodAnalysisExpression) {
                String signature = getSignature(line)
                
                String lineNumbers = line.substring(0, line.indexOf('code') - 1)
                List<Integer> removedLineNumbers = getRemovedLineNumbers(lineNumbers)
                List<Integer> addedLineNumbers = getAddedLineNumbers(lineNumbers)

                Modification modificationType = getModificationType(line) // Changed, added or removed.
              
                insertMethod(modifiedDeclarations, signature, getModifiedLines(removedLineNumbers, addedLineNumbers, modificationType, output, i + 1))
            }
                
        }

        FileManager.delete(ancestorFile)
        FileManager.delete(mergeFile)
        return modifiedDeclarations
    
    }

    private String getSignature(String line) {
        return line.substring(line.indexOf(" in ") + 4)
    }

    private List<Integer> getRemovedLineNumbers(String lineNumbers) {
        for (int i = 0; i < lineNumbers.size(); i++) {
            if(lineNumbers[i] == 'c' || lineNumbers[i] == 'd' || lineNumbers[i] == 'a')
                return parseLines(lineNumbers.substring(0, i))
        }
    }

    private List<Integer> getAddedLineNumbers(String lineNumbers) {
        for (int i = 0; i < lineNumbers.size(); i++) {
            if(lineNumbers[i] == 'c' || lineNumbers[i] == 'd' || lineNumbers[i] == 'a')
                return parseLines(lineNumbers.substring(i + 1))
        }
    }

    private Modification getModificationType(String line) {
        if(line.contains('changed'))
            return Modification.CHANGED
        else if(line.contains('added'))
            return Modification.ADDED
        else
            return Modification.REMOVED
    }

    private Set<ModifiedLine> getModifiedLines(List<Integer> removedLineNumbers, List<Integer> addedLineNumbers, Modification type, String[] outputLines, int start) {
        if(type == Modification.REMOVED)
            return getDeletedLinesTuple(removedLineNumbers, addedLineNumbers, type, outputLines, start)
        else if(type == Modification.ADDED)
            return getAddedLines(addedLineNumbers, type, outputLines, start)
        else 
            return getChangedLines(addedLineNumbers, type, outputLines, start)
    }

    private Set<ModifiedLine> getDeletedLinesTuple(List<Integer> removedLineNumbers, List<Integer> addedLineNumbers, Modification type, String[] outputLines, int iterator) {
        Set<ModifiedLine> modifiedLines = new HashSet<ModifiedLine>()
        for(int i = 0; outputLines[iterator].startsWith('<'); i++) {
            String content = outputLines[iterator].substring(1)
            ModifiedLine modifiedLine = new ModifiedLine(content, new Tuple2(removedLineNumbers[i], addedLineNumbers[0]), type)
            modifiedLines.add(modifiedLine)
            iterator++
        }
        return modifiedLines
    }

    private Set<ModifiedLine> getAddedLines(List<Integer> addedLineNumbers, Modification type, String[] outputLines, int iterator) {
        Set<ModifiedLine> modifiedLines = new HashSet<ModifiedLine>()
        for(int i = 0; isLineModification(outputLines, iterator); iterator++) {
            if(outputLines[iterator].startsWith('>')) {
                String content = outputLines[iterator].substring(1)
                ModifiedLine modifiedLine = new ModifiedLine(content, addedLineNumbers[i], type)
                modifiedLines.add(modifiedLine)
                i++
            }
        }
        return modifiedLines
    }

    private Set<ModifiedLine> getChangedLines(List<Integer> addedLineNumbers, Modification type, String[] outputLines, int iterator) {
        Set<ModifiedLine> modifiedLines = new HashSet<ModifiedLine>()
        for(int i = 0; isLineModification(outputLines, iterator); iterator++) {
            if(!outputLines[i].startsWith('---')) {
                String content = outputLines[iterator].substring(1)
                ModifiedLine modifiedLine = new ModifiedLine(content, addedLineNumbers[i], type)
                modifiedLines.add(modifiedLine)
                if(outputLines[i].startsWith('>'))
                    i++
            }
        }
        return modifiedLines
    }

    private boolean isLineModification(String[] outputLines, int i) {
        return outputLines[i].startsWith('<') || outputLines[i].startsWith('---') || outputLines[i].startsWith('>')
    }

    private ArrayList<Integer> parseLines(String lines) {
        List<Integer> modifiedLines = new ArrayList<Integer>()
        
        int commaIndex = lines.indexOf(',')
        if (commaIndex == -1) 
            modifiedLines.add(Integer.parseInt(lines))
        else {
            int start = Integer.parseInt(lines.substring(0, commaIndex))
            int end = Integer.parseInt(lines.substring(commaIndex + 1))
            for (int i = start; i <= end; i++)
                modifiedLines.add(i)
        }

        return modifiedLines
    }

    private Map<String, ModifiedDeclaration[]> getMethodsIntersection(Set<ModifiedDeclaration> leftMethods, Set<ModifiedDeclaration> rightMethods) {
        Map<String, ModifiedDeclaration[]> intersection = [:]
        for(leftMethod in leftMethods) {
            for(rightMethod in rightMethods) 
                if(leftMethod.equals(rightMethod))
                    intersection.put(leftMethod.getSignature(), [leftMethod, rightMethod])
        }
        return intersection
    }


    private void insertMethod(Set<ModifiedDeclaration> methods, String signature, Set<ModifiedLine> modifiedLines) {
        for (method in methods) {
            if(method.getSignature().equals(signature)) {
                method.addAllLines(modifiedLines)
                return
            }
        }
        methods.add(new ModifiedDeclaration(signature, modifiedLines))
    }

}