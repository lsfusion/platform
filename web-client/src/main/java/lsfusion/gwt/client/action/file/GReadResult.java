package lsfusion.gwt.client.action.file;

import java.io.Serializable;

public class GReadResult implements Serializable {
    public String error;
    public String fileBase64;
    public String name;
    public String extension;

    public GReadResult() {
    }

    public GReadResult(String error, String fileBase64, String name, String extension) {
        this.error = error;
        this.fileBase64 = fileBase64;
        this.name = name;
        this.extension = extension;
    }
}