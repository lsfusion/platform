package platform.interop.navigator;

import platform.interop.form.RemoteFormInterface;

import java.sql.SQLException;

public interface RemoteNavigatorInterface {

    public boolean changeCurrentUser(String login, String password);

    public void addCacheObject(int classID, int value);

    int getDefaultForm(int classId);

    int getCacheObject(int classID);

    public RemoteFormInterface createForm(int formID, boolean currentSession) throws SQLException;

    byte[] getCurrentUserInfoByteArray();

    byte[] getElementsByteArray(int groupID);

    boolean changeCurrentClass(int classID);

    String getCaption(int formID);
    
    public final static int NAVIGATORGROUP_RELEVANTFORM = -2;
    public final static int NAVIGATORGROUP_RELEVANTCLASS = -3;
}
