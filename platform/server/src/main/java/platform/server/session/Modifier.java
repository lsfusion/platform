package platform.server.session;

import platform.server.classes.ValueClass;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.Property;

// вообщем-то вводится для ViewChanges - инкрементного обновления сессии
public interface Modifier<U extends Changes<U>> {
    U used(Property property,U usedChanges);
    U newChanges();

    void modifyAdd(U changes, ValueClass valueClass);
    void modifyRemove(U changes, ValueClass valueClass);
    void modifyData(U changes, DataProperty property);
}
