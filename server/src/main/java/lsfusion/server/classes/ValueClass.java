package lsfusion.server.classes;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.property.IsClassProperty;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ValueClass extends RemoteClass {

    boolean isCompatibleParent(ValueClass remoteClass);

    ValueClassSet getUpSet();

    ResolveClassSet getResolveSet();

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectInstance newInstance(ObjectEntity entity);

    ValueClass getBaseClass();

    String getSID();
    
    String getCaption();

    Object getDefaultValue();

    Stat getTypeStat(boolean forJoin);

    IsClassProperty getProperty();
    
    String getParsedName();
}
