package lsfusion.server.logics.property.infer;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.property.AlgInfoType;
import lsfusion.server.logics.property.AlgType;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

public class InferType implements AlgType {
    public InferType() {
    }

    public static InferInfoType PREVBASE = new InferInfoType();
    public static InferType PREVSAME = new InferType();

    public <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(CalcProperty<P> property) {
        return property.inferClassValueWhere(this);
    }

    public AlgInfoType getAlgInfo() {
        return PREVBASE;
    }
}
