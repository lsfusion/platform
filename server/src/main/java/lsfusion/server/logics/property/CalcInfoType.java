package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImRevMap;

// вычисление empty, full, notnull, классов
public class CalcInfoType extends CalcClassType implements AlgInfoType {

    public <T extends PropertyInterface, P extends PropertyInterface> boolean intersectFull(CalcProperty<T> property, CalcProperty<P> intersect, ImRevMap<P, T> map) {
        return property.calcIntersectFull(intersect, map, this);
    }

    public <P extends PropertyInterface> boolean isEmpty(CalcProperty<P> property) {
        return property.calcEmpty(this);
    }

    public <P extends PropertyInterface> boolean isFull(CalcProperty<P> property, ImCol<P> checkInterfaces) {
        return property.calcFull(checkInterfaces, this);
    }

    public <P extends PropertyInterface> boolean isNotNull(CalcProperty<P> property) {
        return property.calcNotNull(this);
    }

    @Override
    public AlgInfoType getAlgInfo() {
        return this;
    }

}
