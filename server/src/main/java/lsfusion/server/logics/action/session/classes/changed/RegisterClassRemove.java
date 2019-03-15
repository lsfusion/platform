package lsfusion.server.logics.action.session.classes.changed;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.CustomClass;

public interface RegisterClassRemove {

    // должен быть после записи изменений
    void removed(ImSet<CustomClass> classes, long timestamp);

    // должен быть до чтения изменений
    void checked(long timestamp);
    
    boolean removedAfterChecked(CustomClass checkClass, long timestamp); // timestamp - текущий, особо не используется
}
