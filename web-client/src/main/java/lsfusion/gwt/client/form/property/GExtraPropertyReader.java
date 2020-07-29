package lsfusion.gwt.client.form.property;

public abstract class GExtraPropertyReader implements GPropertyReader {

    public int readerID;
    public int groupObjectID;

    public GExtraPropertyReader() {
    }

    private String sID; // optimization

    public GExtraPropertyReader(int readerID, int groupObjectID, String prefix) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
        this.sID = "_PROPERTY_" + prefix + "_" + readerID;
    }

    @Override
    public String getSID() {
        return sID;
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }
}
