package lsfusion.server.logics.property.infer;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.infer.AlgInfoType;
import lsfusion.server.logics.property.infer.CalcClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;

// вычисление empty, full, notnull, классов
public class CalcInfoType extends CalcClassType implements AlgInfoType {

    public CalcInfoType(String caption) {
        super(caption);
    }

    public <P extends PropertyInterface> boolean isEmpty(CalcProperty<P> property) {
        return property.calcEmpty(this);
    }

    public <P extends PropertyInterface> boolean isFull(CalcProperty<P> property, ImCol<P> checkInterfaces) {
        return property.calcFull(checkInterfaces, this);
    }

    public <P extends PropertyInterface> boolean isNotNull(ImSet<P> checkInterfaces, CalcProperty<P> property) {
        return property.calcNotNull(checkInterfaces, this);
    }

    @Override
    public AlgInfoType getAlgInfo() {
        return this;
    }

}
