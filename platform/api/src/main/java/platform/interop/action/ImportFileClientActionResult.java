package platform.interop.action;

public class ImportFileClientActionResult extends ClientActionResult {

    public boolean fileExists = true;
    public String fileContent;

    public ImportFileClientActionResult(boolean fileExists, String fileContent) {
        this.fileExists = fileExists;
        this.fileContent = fileContent;
    }
}
