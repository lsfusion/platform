package lsfusion.gwt.client.action.file;

import java.io.Serializable;

public class GReadResult implements Serializable {
    public String fileBase64;
    public String extension;

    public GReadResult() {
    }

    public GReadResult(String fileBase64, String extension) {
        this.fileBase64 = fileBase64;
        this.extension = extension;
    }
}