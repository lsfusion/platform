package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.BaseExpr;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.group.AbstractGroup;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ValueClass extends RemoteClass {

    boolean isCompatibleParent(ValueClass remoteClass);

    AbstractGroup getParent();

    AndClassSet getUpSet();

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectInstance newObject(int ID, String SID, String caption, CustomClassListener classListener, boolean addOnTransaction);

    // получает выражение чисто для получения класса
    BaseExpr getClassExpr();
}
