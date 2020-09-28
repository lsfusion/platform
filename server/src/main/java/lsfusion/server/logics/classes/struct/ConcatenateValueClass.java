package lsfusion.server.logics.classes.struct;

import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.classes.user.set.ResolveConcatenateClassSet;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ConcatenateValueClass implements ValueClass {
    
    private final ValueClass[] valueClasses;
    public ConcatenateValueClass(ValueClass[] valueClasses) {
        this.valueClasses = valueClasses;
    }
    
    public ValueClass get(int i) {
        return valueClasses[i];
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        throw new RuntimeException("not supported");
    }

    public ValueClassSet getUpSet() {
        ValueClassSet[] upClasses = new ValueClassSet[valueClasses.length];
        for(int i=0;i<valueClasses.length;i++)
            upClasses[i] = valueClasses[i].getUpSet();
        return new ConcatenateClassSet(upClasses);
    }

    public ResolveClassSet getResolveSet() {
        ResolveClassSet[] upClasses = new ResolveClassSet[valueClasses.length];
        for(int i=0;i<valueClasses.length;i++)
            upClasses[i] = valueClasses[i].getResolveSet();
        return new ResolveConcatenateClassSet(upClasses);
    }

    public void serialize(DataOutputStream outStream) {
        throw new RuntimeException("not supported");
    }

    public ObjectInstance newInstance(ObjectEntity entity) {
        throw new RuntimeException("not supported");
    }

    public ValueClass getBaseClass() {
        ValueClass[] result = new ValueClass[valueClasses.length];
        for (int i = 0; i < valueClasses.length; i++) {
            result[i] = valueClasses[i].getBaseClass();
        }
        return new ConcatenateValueClass(result);
    }

    public String getSID() {
        String sID = "CONCAT";
        for(ValueClass valueClass : valueClasses)
            sID = sID + "_" + valueClass.getSID();
        return sID;
    }

    public LocalizedString getCaption() {
        throw new RuntimeException("not supported");
    }

    public Object getDefaultValue() {
        throw new RuntimeException("not supported");
    }

    @Override
    public LA getDefaultOpenAction(BusinessLogics BL) {
        return null;
    }

    public Stat getTypeStat(boolean forJoin) {
        throw new RuntimeException("not supported");
    }

    public IsClassProperty getProperty() {
        throw new RuntimeException("not supported");
    }

    public Type getType() {
        Type[] types = new Type[valueClasses.length];
        for(int i=0;i<valueClasses.length;i++)
            types[i] = valueClasses[i].getType();
        return ConcatenateType.get(types);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ConcatenateValueClass && Arrays.equals(valueClasses, ((ConcatenateValueClass) o).valueClasses);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(valueClasses);
    }

    public String getParsedName() {
        return null;
    }
}
