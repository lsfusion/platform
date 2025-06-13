package lsfusion.gwt.client.action.file;

import java.io.Serializable;

public class GReadResult implements Serializable {
    public String error;
    public String fileBase64;
    public String extension;

    public GReadResult() {
    }

    public GReadResult(String error, String fileBase64, String extension) {
        this.error = error;
        this.fileBase64 = fileBase64;
        this.extension = extension;
    }
}