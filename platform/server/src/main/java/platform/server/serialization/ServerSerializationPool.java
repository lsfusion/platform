package platform.server.serialization;

import platform.interop.serialization.SerializationPool;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotFilterEntity;
import platform.server.form.entity.filter.OrFilterEntity;
import platform.server.form.entity.filter.PropertyFilterEntity;
import platform.server.logics.DataObject;
import platform.server.logics.property.ObjectValueProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.SelectionProperty;
import platform.server.logics.property.actions.AddObjectActionProperty;
import platform.server.logics.property.actions.ImportFromExcelActionProperty;

public class ServerSerializationPool extends SerializationPool {
    public ServerSerializationPool() {
        this(null);
    }

    public ServerSerializationPool(Object context) {
        super(context);
        //порядок добавления должен соответствовать порядку в ClientSerializationPool

        addMapping(FormEntity.class);
        addMapping(GroupObjectEntity.class);
        addMapping(PropertyDrawEntity.class);
        addMapping(Property.class);
        addMapping(ObjectEntity.class);
        addMapping(PropertyObjectEntity.class);
        addMapping(PropertyFilterEntity.class);
        addMapping(OrFilterEntity.class);
        addMapping(NotFilterEntity.class);
        addMapping(CompareFilterEntity.class);

        addMapping(CurrentComputerEntity.class);
        addMapping(DataObject.class);

        addMapping(AddObjectActionProperty.class);
        addMapping(ImportFromExcelActionProperty.class);
        addMapping(ObjectValueProperty.class);
        addMapping(SelectionProperty.class);
    }
}
