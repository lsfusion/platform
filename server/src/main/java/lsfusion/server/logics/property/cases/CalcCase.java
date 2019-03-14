package lsfusion.server.logics.property.cases;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class CalcCase<T extends PropertyInterface> extends Case<T, PropertyInterfaceImplement<T>, PropertyInterfaceImplement<T>> {
    
    public CalcCase(PropertyInterfaceImplement<T> where, PropertyInterfaceImplement<T> property) {
        super(where, property);
    }

    public CalcCase(AbstractCalcCase<T> aCase) {
        super(aCase.where, aCase.implement);
    }    

    public boolean isSimple() { // дебильновато конечно, но не хочется классы плодить пока
        return where == implement;
    }

    public boolean isClassSimple() { // дебильновато конечно, но не хочется классы плодить пока
        return implement instanceof PropertyMapImplement && ((PropertyMapImplement) implement).mapClassProperty().equalsMap(where);
    }

    public <P extends PropertyInterface> CalcCase<P> map(ImRevMap<T, P> map) {
        return new CalcCase<>(where.map(map), implement.map(map));
    }
}
