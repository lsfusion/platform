package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.IsClassProperty;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ValueClass extends RemoteClass {

    boolean isCompatibleParent(ValueClass remoteClass);

    AndClassSet getUpSet();

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectInstance newInstance(ObjectEntity entity);

    ValueClass getBaseClass();

    String getSID();

    String getCaption();

    Object getDefaultValue();

    Stat getTypeStat();

    IsClassProperty getProperty();
}
