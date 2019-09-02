package services.modifiedlines

class MutuallyModifiedMethod {

    private String signature
    
    private Set<ModifiedLine> leftAddedLines
    private Set<ModifiedLine> leftDeletedLines

    private Set<ModifiedLine> rightAddedLines
    private Set<ModifiedLine> rightDeletedLines

    MutuallyModifiedMethod (signature) {
        this.signature = signature
        
        this.leftAddedLines = new Set<ModifiedLine>()
        this.leftDeletedLines = new Set<ModifiedLine>()

        this.rightAddedLines = new Set<ModifiedLine>()
        this.rightDeletedLines = new Set<ModifiedLine>()
    }

    public void addLeftAddedLine (ModifiedLine line) {
        this.leftAddedLines.add(line)
    }

    public void addLeftDeletedLine (ModifiedLine line) {
        this.leftDeletedLines.add(line)
    }

    public void addRightAddedLine (ModifiedLine line) {
        this.rightAddedLines.add(line)
    }

    public void addRightDeletedLine (ModifiedLine line) {
        this.rightAddedLines.add(line)
    }

    public Set<ModifiedLine> getLeftAddedLines() {
        return this.leftAddedLines
    }

    public Set<ModifiedLine> getLeftDeletedLines() {
        return this.leftDeletedLines
    }

    public Set<ModifiedLine> getRightAddedLines() {
        return this.rightAddedLines
    }

    public set<ModifiedLine> getRightDeletedLines() {
        return this.rightDeletedLines
    }

}