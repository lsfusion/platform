package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.IsClassProperty;

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

    public void serialize(DataOutputStream outStream) throws IOException {
        throw new RuntimeException("not supported");
    }

    public ObjectInstance newInstance(ObjectEntity entity) {
        throw new RuntimeException("not supported");
    }

    public ValueClass getBaseClass() {
        throw new RuntimeException("not supported");
    }

    public String getSID() {
        String sID = "";
        for(ValueClass valueClass : valueClasses)
            sID = (sID.length()==0?"":sID+"_")  + valueClass.getSID();
        return sID;
    }

    public String getCaption() {
        throw new RuntimeException("not supported");
    }

    public Object getDefaultValue() {
        throw new RuntimeException("not supported");
    }

    public Stat getTypeStat() {
        throw new RuntimeException("not supported");
    }

    public IsClassProperty getProperty() {
        throw new RuntimeException("not supported");
    }

    public Type getType() {
        Type[] types = new Type[valueClasses.length];
        for(int i=0;i<valueClasses.length;i++)
            types[i] = valueClasses[i].getType();
        return new ConcatenateType(types);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ConcatenateValueClass && Arrays.equals(valueClasses, ((ConcatenateValueClass) o).valueClasses);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(valueClasses);
    }
}
