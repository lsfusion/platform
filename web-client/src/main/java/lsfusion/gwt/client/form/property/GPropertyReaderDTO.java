package lsfusion.gwt.client.form.property;

import java.io.Serializable;

public class GPropertyReaderDTO implements Serializable {
    public int readerID;
    public byte type;

    public GPropertyReaderDTO(){}

    public GPropertyReaderDTO(int readerID, byte type) {
        this.readerID = readerID;
        this.type = type;
    }
}
