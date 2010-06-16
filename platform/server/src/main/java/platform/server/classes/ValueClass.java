package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.ValueExpr;
import platform.server.logics.DataObject;
import platform.server.logics.property.group.AbstractGroup;
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

    // получает рандомный объект
    DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException;

    List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects);

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectImplement newObject(int ID, String SID, String caption, CustomClassView classView, boolean addOnTransaction);

    ValueExpr getActionExpr(); 
}
