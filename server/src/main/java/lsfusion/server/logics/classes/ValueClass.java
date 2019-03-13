package lsfusion.server.logics.classes;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.IsClassProperty;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;

public interface ValueClass extends RemoteClass {

    boolean isCompatibleParent(ValueClass remoteClass);

    ValueClassSet getUpSet();

    ResolveClassSet getResolveSet();

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectInstance newInstance(ObjectEntity entity);

    ValueClass getBaseClass();

    String getSID();
    
    LocalizedString getCaption();

    Object getDefaultValue();

    Stat getTypeStat(boolean forJoin);

    IsClassProperty getProperty();
    
    String getParsedName();
    
    Comparator<ValueClass> comparator = new Comparator<ValueClass>() {
        public int compare(ValueClass o1, ValueClass o2) {
            String sid1 = o1.getSID();
            String sid2 = o2.getSID();
            return sid1.compareTo(sid2);
        }
    };
}
