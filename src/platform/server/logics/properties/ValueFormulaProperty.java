package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.classes.DataClass;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.ValueClassSet;

// вообще Collection
abstract class ValueFormulaProperty<T extends FormulaPropertyInterface> extends FormulaProperty<T> {

    DataClass Value;

    ValueFormulaProperty(TableFactory iTableFactory, DataClass iValue) {
        super(iTableFactory);
        Value = iValue;
    }

    public ClassSet calculateValueClass(InterfaceClass<T> ClassImplement) {
        if(ClassImplement.hasEmpty()) return new ClassSet();
        return ClassSet.getUp(Value);
    }

    public InterfaceClassSet<T> calculateClassSet(ClassSet reqValue) {

        if(reqValue.intersect(ClassSet.getUp(Value)))
            return getOperandInterface();
        else
            return new InterfaceClassSet<T>();
    }

    public ValueClassSet<T> calculateValueClassSet() {
        return new ValueClassSet<T>(ClassSet.getUp(Value),getOperandInterface());
    }

    abstract DataClass getOperandClass();

    InterfaceClassSet<T> getOperandInterface() {
        InterfaceClass<T> Result = new InterfaceClass<T>();
        for(T Interface : interfaces)
            Result.put(Interface,ClassSet.getUp(getOperandClass()));
        return new InterfaceClassSet<T>(Result);
    }

}
