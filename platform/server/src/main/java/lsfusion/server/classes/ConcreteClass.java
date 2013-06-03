package lsfusion.server.classes;

import lsfusion.base.GlobalObject;
import lsfusion.server.classes.sets.AndClassSet;

public interface ConcreteClass extends RemoteClass, AndClassSet, GlobalObject {

    // у всех одинаковая реализация - проблема множественного наследования
    boolean inSet(AndClassSet set);

}
