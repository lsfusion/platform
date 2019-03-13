package lsfusion.server.logics.classes;

import lsfusion.base.comb.map.GlobalObject;
import lsfusion.server.logics.classes.sets.AndClassSet;

public interface ConcreteClass extends RemoteClass, AndClassSet, GlobalObject {

    // у всех одинаковая реализация - проблема множественного наследования
    boolean inSet(AndClassSet set);

    String getShortName();
}
