package lsfusion.gwt.client.action;

import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;

import java.io.Serializable;

public class GListFilesResult implements Serializable {
    public String error;
    public String[] names;
    public Boolean[] isDirectory;
    public GDateTimeDTO[] modifiedDateTime;
    public Long[] fileSize;

    public GListFilesResult() {
    }

    public GListFilesResult(String error, String[] names, Boolean[] isDirectory, GDateTimeDTO[] modifiedDateTime, Long[] fileSize) {
        this.error = error;
        this.names = names;
        this.isDirectory = isDirectory;
        this.modifiedDateTime = modifiedDateTime;
        this.fileSize = fileSize;
    }
}