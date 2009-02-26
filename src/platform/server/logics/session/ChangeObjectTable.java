package platform.server.logics.session;

import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.types.Type;

import java.util.ArrayList;
import java.util.Collection;

public class ChangeObjectTable extends ChangeTable {

    public Collection<KeyField> objects;
    public KeyField property;
    public PropertyField value;

    ChangeObjectTable(String tablePrefix,Integer iObjects, Type iDBType) {
        super(tablePrefix+"changetable"+iObjects+"t"+iDBType.ID);

        objects = new ArrayList<KeyField>();
        for(Integer i=0;i<iObjects;i++) {
            KeyField objKeyField = new KeyField("object"+i, Type.object);
            objects.add(objKeyField);
            keys.add(objKeyField);
        }

        property = new KeyField("property",Type.system);
        keys.add(property);

        value = new PropertyField("value",iDBType);
        properties.add(value);
    }
}
