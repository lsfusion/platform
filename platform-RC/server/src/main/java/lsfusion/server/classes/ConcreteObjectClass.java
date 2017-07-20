package lsfusion.server.classes;

import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.classes.sets.ObjectClassSet;
import lsfusion.server.logics.ObjectValue;

public interface ConcreteObjectClass extends ConcreteClass,ObjectClass,ObjectClassSet {

    void getDiffSet(ConcreteObjectClass diffClass, MSet<CustomClass> mAddClasses, MSet<CustomClass> mRemoveClasses);

    ObjectValue getClassObject();

}
