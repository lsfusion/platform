package lsfusion.server.logics.classes.user;

import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;

public interface ConcreteObjectClass extends ConcreteClass,ObjectClass,ObjectClassSet {

    void getDiffSet(ConcreteObjectClass diffClass, MSet<CustomClass> mAddClasses, MSet<CustomClass> mRemoveClasses);

    ObjectValue getClassObject();

}
