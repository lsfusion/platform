package platform.server.classes;

import platform.base.Result;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.logics.ObjectValue;

public interface ConcreteObjectClass extends ConcreteClass,ObjectClass,ObjectClassSet {

    public abstract void getDiffSet(ConcreteObjectClass diffClass, MSet<CustomClass> mAddClasses, MSet<CustomClass> mRemoveClasses);

    public abstract ObjectValue getClassObject();

}
