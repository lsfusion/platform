package lsfusion.server.logics.property.cases;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.implement.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.CalcPropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class CalcCase<T extends PropertyInterface> extends Case<T, CalcPropertyInterfaceImplement<T>, CalcPropertyInterfaceImplement<T>> {
    
    public CalcCase(CalcPropertyInterfaceImplement<T> where, CalcPropertyInterfaceImplement<T> property) {
        super(where, property);
    }

    public CalcCase(AbstractCalcCase<T> aCase) {
        super(aCase.where, aCase.implement);
    }    

    public boolean isSimple() { // дебильновато конечно, но не хочется классы плодить пока
        return where == implement;
    }

    public boolean isClassSimple() { // дебильновато конечно, но не хочется классы плодить пока
        return implement instanceof CalcPropertyMapImplement && ((CalcPropertyMapImplement) implement).mapClassProperty().equalsMap(where);
    }

    public <P extends PropertyInterface> CalcCase<P> map(ImRevMap<T, P> map) {
        return new CalcCase<>(where.map(map), implement.map(map));
    }
}
