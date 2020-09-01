package lsfusion.gwt.client.form.property;

public abstract class GRowPropertyReader implements GPropertyReader {
    public int readerID;

    public GRowPropertyReader() {
    }

    private String sID;

    public GRowPropertyReader(int readerID, String prefix) {
        this.readerID = readerID;
        this.sID = "_ROW_" + prefix + "_" + readerID;
    }

    @Override
    public String getNativeSID() {
        return sID;
    }

    @Override
    public int getGroupObjectID() {
        return readerID;
    }
}
