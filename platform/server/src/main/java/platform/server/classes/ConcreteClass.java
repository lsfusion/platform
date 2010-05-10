package platform.server.classes;

import platform.server.classes.sets.AndClassSet;

public interface ConcreteClass extends RemoteClass, AndClassSet {

    // у всех одинаковая реализация - проблема множественного наследования
    boolean inSet(AndClassSet set);
    
}
