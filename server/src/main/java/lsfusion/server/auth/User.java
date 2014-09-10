package lsfusion.server.auth;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class User extends PolicyAgent {

    public final int ID;

    public User(int ID) {

        this.ID = ID;
    }

    List<UserGroup> userGroups = new ArrayList();

    public SecurityPolicy getSecurityPolicy() {

        SecurityPolicy resultPolicy = new SecurityPolicy();

        for (UserGroup userGroup : userGroups)
            resultPolicy.override(userGroup.getSecurityPolicy());

        resultPolicy.override(super.getSecurityPolicy());
        return resultPolicy;
    }
    
    public DataObject getDataObject(ConcreteCustomClass customClass, DataSession session) throws SQLException, SQLHandledException {
        return session.getDataObject(customClass, ID);
    }
    
    public int timeout = 0;
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getTimeout() {
        return timeout;
    }
}
