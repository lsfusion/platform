package platform.server.data.classes;

import platform.server.data.classes.where.AndClassSet;
import platform.server.logics.DataObject;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.session.SQLSession;
import platform.server.view.form.CustomClassView;
import platform.server.view.form.ObjectImplement;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;

public interface ValueClass extends RemoteClass {

    boolean isCompatibleParent(ValueClass remoteClass);

    AbstractGroup getParent();

    AndClassSet getUpSet();

    DataProperty getExternalID();

    // получает рандомный объект
    DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException;

    List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects);

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectImplement newObject(int ID, String SID, String caption, CustomClassView classView);
}
