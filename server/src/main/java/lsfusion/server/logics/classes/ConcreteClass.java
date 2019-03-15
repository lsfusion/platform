package lsfusion.server.logics.classes;

import lsfusion.base.comb.map.GlobalObject;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public interface ConcreteClass extends AClass, AndClassSet, GlobalObject {

    // у всех одинаковая реализация - проблема множественного наследования
    boolean inSet(AndClassSet set);

    String getShortName();
}
