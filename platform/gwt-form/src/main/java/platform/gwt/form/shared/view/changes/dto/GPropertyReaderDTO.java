package platform.gwt.form.shared.view.changes.dto;

import java.io.Serializable;

public class GPropertyReaderDTO implements Serializable {
    public int readerID;
    public int groupObjectID;
    public byte type;

    public GPropertyReaderDTO(){}

    public GPropertyReaderDTO(int readerID, int groupObjectID, byte type) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
        this.type = type;
    }
}
