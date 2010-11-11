package platform.interop.action;

import java.io.Serializable;

public class ImportFileClientActionResult implements Serializable {

    public boolean fileExists = true;
    public String fileContent;

    public ImportFileClientActionResult(boolean fileExists, String fileContent) {
        this.fileExists = fileExists;
        this.fileContent = fileContent;
    }
}
