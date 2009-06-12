package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.where.ClassWhere;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;

import java.util.Map;

public class DataChangeTable extends ChangePropertyTable<DataPropertyInterface,DataChangeTable> {

    public DataChangeTable(DataProperty property) {
        super("data",property);
    }

    public DataChangeTable(String iName, Map<KeyField, DataPropertyInterface> iMapKeys, PropertyField iValue, ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iMapKeys, iValue, iClasses, iPropertyClasses);
    }

    public DataChangeTable createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        return new DataChangeTable(name, mapKeys, value, iClasses, iPropertyClasses);
    }
}
