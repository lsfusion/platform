package platform.server.classes;

import platform.base.GlobalObject;
import platform.server.classes.sets.AndClassSet;

public interface ConcreteClass extends RemoteClass, AndClassSet, GlobalObject {

    // у всех одинаковая реализация - проблема множественного наследования
    boolean inSet(AndClassSet set);

}
