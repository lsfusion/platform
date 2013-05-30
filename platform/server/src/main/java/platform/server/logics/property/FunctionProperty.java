package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.session.StructChanges;

// свойство производное от остальных свойств
public abstract class FunctionProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    protected FunctionProperty(String sID, String caption, ImOrderSet<T> interfaces) {
        super(sID, caption, interfaces);
    }

    public static void fillDepends(MSet<CalcProperty> depends, ImCol<? extends CalcPropertyInterfaceImplement> propImplements) {
        for(CalcPropertyInterfaceImplement propImplement : propImplements)
            propImplement.mapFillDepends(depends);
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getDepends());
    }
}
