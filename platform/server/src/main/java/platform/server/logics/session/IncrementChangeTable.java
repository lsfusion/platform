package platform.server.logics.session;

import platform.server.data.PropertyField;
import platform.server.data.types.Type;

public class IncrementChangeTable extends ChangeObjectTable {

    public PropertyField prevValue;

    public IncrementChangeTable(Integer iObjects, Type iDBType) {
        super("inc",iObjects,iDBType);

        prevValue = new PropertyField("prevvalue",iDBType);
        properties.add(prevValue);
    }
}
