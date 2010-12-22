package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.base.GlobalObject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public interface ConcreteClass extends RemoteClass, AndClassSet, GlobalObject {

    // у всех одинаковая реализация - проблема множественного наследования
    boolean inSet(AndClassSet set);

}
