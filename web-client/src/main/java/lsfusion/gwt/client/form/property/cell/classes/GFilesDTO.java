package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;
import java.util.ArrayList;

public class GFilesDTO implements Serializable {
    public ArrayList<String> filePaths;
    public boolean multiple;
    public boolean storeName;
    public boolean custom;
    public boolean named;

    public GFilesDTO() {
    }

    public GFilesDTO(ArrayList<String> filePaths, boolean multiple, boolean storeName, boolean custom, boolean named) {
        this.filePaths = filePaths;
        this.multiple = multiple;
        this.storeName = storeName;
        this.custom = custom;
        this.named = named;
    }
}
