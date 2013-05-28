package platform.gwt.form.shared.view.changes.dto;

import java.io.Serializable;
import java.util.ArrayList;

public class GFilesDTO implements Serializable {
    public ArrayList<String> filePaths;
    public boolean multiple;
    public boolean custom;

    public GFilesDTO() {
    }

    public GFilesDTO(ArrayList<String> filePaths, boolean multiple, boolean custom) {
        this.filePaths = filePaths;
        this.multiple = multiple;
        this.custom = custom;
    }
}
