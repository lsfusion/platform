package platform.server.classes;

import platform.server.classes.sets.ObjectClassSet;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.Collection;

public interface ConcreteObjectClass extends ConcreteClass,ObjectClass,ObjectClassSet {

    public abstract void getDiffSet(ConcreteObjectClass diffClass, Collection<CustomClass> addClasses,Collection<CustomClass> removeClasses);

    public abstract ObjectValue getClassObject();

}
