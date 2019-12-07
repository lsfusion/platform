package lsfusion.gwt.client.form.property;

import java.io.Serializable;

public class GPropertyReaderDTO implements Serializable {
    public int readerID;
    public byte type;
    public int index;

    public GPropertyReaderDTO(){}

    public GPropertyReaderDTO(int readerID, byte type, int index) {
        this.readerID = readerID;
        this.type = type;
        this.index = index;
    }
}
