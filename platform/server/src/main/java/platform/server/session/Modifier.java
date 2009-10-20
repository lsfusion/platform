package platform.server.session;

import platform.server.data.classes.ValueClass;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.Property;

// вообщем-то вводится для ViewDataChanges - инкрементного обновления сессии
public interface Modifier<U extends DataChanges<U>> {
    U used(Property property,U usedChanges);
    U newChanges();

    void modifyAdd(U changes, ValueClass valueClass);
    void modifyRemove(U changes, ValueClass valueClass);
    void modifyData(U changes, DataProperty property);
}
