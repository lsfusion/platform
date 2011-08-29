package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.query.Stat;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.ObjectInstance;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ValueClass extends RemoteClass {

    boolean isCompatibleParent(ValueClass remoteClass);

    AndClassSet getUpSet();

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectInstance newInstance(ObjectEntity entity);

    // получает выражение чисто для получения класса
    BaseExpr getClassExpr();

    ValueClass getBaseClass();

    String getSID();

    String getCaption();

    Object getDefaultValue();

    Stat getTypeStat();
}
