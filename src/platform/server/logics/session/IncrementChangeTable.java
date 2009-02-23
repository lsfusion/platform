package platform.server.logics.session;

import platform.server.data.types.Type;
import platform.server.data.PropertyField;

public class IncrementChangeTable extends ChangeObjectTable {

    public PropertyField prevValue;

    public IncrementChangeTable(Integer iObjects, Type iDBType) {
        super("inc",iObjects,iDBType);

        prevValue = new PropertyField("prevvalue",iDBType);
        properties.add(prevValue);
    }
}
