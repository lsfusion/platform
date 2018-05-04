package lsfusion.server.session;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ValueClass;

public interface RegisterClassRemove {

    // должен быть после записи изменений
    void removed(ImSet<ValueClass> classes, long timestamp);

    // должен быть до чтения изменений
    void checked(long timestamp);
    
    boolean removedAfterChecked(ValueClass checkClass, long timestamp); // timestamp - текущий, особо не используется
}
