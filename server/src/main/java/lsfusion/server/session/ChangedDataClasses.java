package lsfusion.server.session;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.server.logics.classes.ConcreteObjectClass;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.property.ClassDataProperty;

public class ChangedDataClasses {

    public static SymmAddValue<ClassDataProperty, ChangedDataClasses> mergeAdd = new SymmAddValue<ClassDataProperty, ChangedDataClasses>() {
        public ChangedDataClasses addValue(ClassDataProperty key, ChangedDataClasses prevValue, ChangedDataClasses newValue) {
            return prevValue.merge(newValue);
        }
    };
    public final ImSet<CustomClass> add; // используется для Debugger'а и логирования, и getIsClassChange
    public final ImSet<CustomClass> remove; // используется для Debugger'а, логирования, getIsClassChange + packRemoveClasses / dropDataChanges
    public final ImSet<ConcreteObjectClass> old; // используется для changeClass
    public final ImSet<ConcreteObjectClass> newc; // используется для changeClass и многих других оптимизаций (в основном isValueClass) 

    public ChangedDataClasses(ImSet<CustomClass> add, ImSet<CustomClass> remove, ImSet<ConcreteObjectClass> old, ImSet<ConcreteObjectClass> newc) {
        this.add = add;
        this.remove = remove;
        this.old = old;
        this.newc = newc;
    }
    
    public static final ChangedDataClasses EMPTY = new ChangedDataClasses(SetFact.<CustomClass>EMPTY(), SetFact.<CustomClass>EMPTY(), SetFact.<ConcreteObjectClass>EMPTY(), SetFact.<ConcreteObjectClass>EMPTY());

    public ChangedDataClasses merge(ChangedDataClasses dataChangedClasses) {
        if(this == EMPTY)
            return dataChangedClasses;
        
        if(newc.containsAll(dataChangedClasses.newc) && remove.containsAll(dataChangedClasses.remove) && old.containsAll(dataChangedClasses.old) && add.containsAll(dataChangedClasses.add))
            return this;
        if(dataChangedClasses.newc.containsAll(newc) && dataChangedClasses.remove.containsAll(remove) && dataChangedClasses.old.containsAll(old) && dataChangedClasses.add.containsAll(add))
            return dataChangedClasses;
        return new ChangedDataClasses(add.merge(dataChangedClasses.add), remove.merge(dataChangedClasses.remove), old.merge(dataChangedClasses.old), newc.merge(dataChangedClasses.newc));
    }
}
