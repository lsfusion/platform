package platform.interop.form;

import java.sql.SQLException;

public interface RemoteFormInterface {

    byte[] getReportDesignByteArray();
    byte[] getReportDataByteArray() throws SQLException;

    int getGID();

    byte[] getRichDesignByteArray();

    void gainedFocus();

    byte[] getFormChangesByteArray() throws SQLException;

    void changeGroupObject(int groupID, byte[] value) throws SQLException;

    int getObjectClassID(int objectID);

    void changeGroupObject(int groupID, int changeType) throws SQLException;

    void changePropertyView(int propertyID, byte[] object, boolean externalID) throws SQLException;

    void changeObject(int objectID, Integer value) throws SQLException;

    void addObject(int objectID, int classID) throws SQLException;

    void changeClass(int objectID, int classID) throws SQLException;

    void changeGridClass(int objectID,int idClass) throws SQLException;

    void switchClassView(int groupID) throws SQLException;

    void changeOrder(int propertyID, int modiType);

    void clearUserFilters();

    void addFilter(byte[] state);

    void setRegularFilter(int groupID, int filterID);

    int getID();

    void refreshData();

    boolean hasSessionChanges();

    String saveChanges() throws SQLException;

    void cancelChanges() throws SQLException;

    byte[] getBaseClassByteArray(int objectID);

    byte[] getChildClassesByteArray(int objectID, int classID);

    public byte[] getPropertyEditorObjectValueByteArray(int propertyID, boolean externalID);

    public static int GID_SHIFT = 1000;

    final public static int CHANGEGROUPOBJECT_FIRSTROW = 0;
    final public static int CHANGEGROUPOBJECT_LASTROW = 1;
    
    final public static int ORDER_REPLACE = 1;
    final public static int ORDER_ADD = 2;
    final public static int ORDER_REMOVE = 3;
    final public static int ORDER_DIR = 4;

}
