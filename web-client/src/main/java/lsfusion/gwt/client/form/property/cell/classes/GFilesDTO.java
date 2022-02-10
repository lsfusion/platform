package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;

public class GFilesDTO implements Serializable {
    public String filePath;
    public String fileName;
    public boolean storeName;
    public boolean custom;
    public boolean named;

    public GFilesDTO() {
    }

    public GFilesDTO(String filePath, String fileName, boolean storeName, boolean custom, boolean named) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.storeName = storeName;
        this.custom = custom;
        this.named = named;
    }
}
