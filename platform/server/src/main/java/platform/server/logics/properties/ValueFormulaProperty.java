package platform.server.logics.properties;

import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.data.TableFactory;

// вообще Collection
abstract class ValueFormulaProperty<T extends FormulaPropertyInterface> extends FormulaProperty<T> {

    RemoteClass value;

    ValueFormulaProperty(TableFactory iTableFactory, RemoteClass iValue) {
        super(iTableFactory);
        value = iValue;
    }

    public ClassSet calculateValueClass(InterfaceClass<T> classImplement) {
        if(classImplement.hasEmpty()) return new ClassSet();
        return ClassSet.getUp(value);
    }

    public InterfaceClassSet<T> calculateClassSet(ClassSet reqValue) {

        if(reqValue.intersect(ClassSet.getUp(value)))
            return getOperandInterface();
        else
            return new InterfaceClassSet<T>();
    }

    public ValueClassSet<T> calculateValueClassSet() {
        return new ValueClassSet<T>(ClassSet.getUp(value),getOperandInterface());
    }

    abstract RemoteClass getOperandClass();

    InterfaceClassSet<T> getOperandInterface() {
        InterfaceClass<T> result = new InterfaceClass<T>();
        for(T Interface : interfaces)
            result.put(Interface,ClassSet.getUp(getOperandClass()));
        return new InterfaceClassSet<T>(result);
    }

}
