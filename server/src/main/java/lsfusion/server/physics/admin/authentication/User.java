package lsfusion.server.physics.admin.authentication;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class User {

    public final long ID;

    public User(long ID) {

        this.ID = ID;
    }

    public DataObject getDataObject(ConcreteCustomClass customClass, DataSession session) throws SQLException, SQLHandledException {
        return session.getDataObject(customClass, ID);
    }
}
