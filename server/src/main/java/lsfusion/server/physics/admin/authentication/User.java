package lsfusion.server.physics.admin.authentication;

import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ConcreteCustomClass;

import java.sql.SQLException;

public class User {

    public final long ID;

    public User(long ID) {

        this.ID = ID;
    }

    public DataObject getDataObject(ConcreteCustomClass customClass, DataSession session) throws SQLException, SQLHandledException {
        return session.getDataObject(customClass, ID);
    }
}
