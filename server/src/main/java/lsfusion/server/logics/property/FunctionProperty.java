package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.session.StructChanges;

// свойство производное от остальных свойств
public abstract class FunctionProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    protected FunctionProperty(String caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    public static void fillDepends(MSet<CalcProperty> depends, ImCol<? extends CalcPropertyInterfaceImplement> propImplements) {
        for(CalcPropertyInterfaceImplement propImplement : propImplements)
            propImplement.mapFillDepends(depends);
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getDepends());
    }
}
