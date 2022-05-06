package lsfusion.server.logics.classes;

import lsfusion.server.data.stat.Stat;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;

public interface ValueClass extends AClass {

    boolean isCompatibleParent(ValueClass remoteClass);

    ValueClassSet getUpSet();

    ResolveClassSet getResolveSet();

    void serialize(DataOutputStream outStream) throws IOException;

    ObjectInstance newInstance(ObjectEntity entity);

    ValueClass getBaseClass();

    String getSID();
    
    LocalizedString getCaption();

    Object getDefaultValue();

    LA getDefaultOpenAction(BaseLogicsModule baseLM);

    Stat getTypeStat(boolean forJoin);

    IsClassProperty getProperty();
    
    String getParsedName();

    default ValueClass getFilterMatchValueClass() {
        return this;
    }

    Comparator<ValueClass> comparator = (o1, o2) -> {
        String sid1 = o1.getSID();
        String sid2 = o2.getSID();
        return sid1.compareTo(sid2);
    };
}
