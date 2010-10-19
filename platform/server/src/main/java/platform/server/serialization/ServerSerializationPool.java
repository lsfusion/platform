package platform.server.serialization;

import platform.interop.serialization.SerializationPool;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.view.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.group.AbstractGroup;


public class ServerSerializationPool extends SerializationPool<ServerContext> {
    public ServerSerializationPool() {
        this(null);
    }

    public ServerSerializationPool(ServerContext context) {
        super(context);
        //порядок добавления должен соответствовать порядку в ClientSerializationPool

        addMapping2(FormEntity.class);
        addMapping2(GroupObjectEntity.class);
        addMapping2(PropertyDrawEntity.class);
        addMapping2(Property.class);
        addMapping2(AbstractGroup.class);

        addMapping2(PropertyFilterEntity.class);
        addMapping2(CompareFilterEntity.class);
        addMapping2(IsClassFilterEntity.class);
        addMapping2(NotNullFilterEntity.class);

        addMapping2(OrFilterEntity.class);
        addMapping2(NotFilterEntity.class);

        addMapping2(RegularFilterEntity.class);
        addMapping2(RegularFilterGroupEntity.class);

        addMapping2(PropertyInterface.class);

        addMapping2(DataObject.class);
        addMapping2(CurrentComputerEntity.class);
        addMapping2(ObjectEntity.class);
        addMapping2(PropertyObjectEntity.class);

        addMapping2(FormView.class);
        addMapping2(ComponentView.class);
        addMapping2(ContainerView.class);
        addMapping2(GroupObjectView.class);
        addMapping2(ShowTypeView.class);
        addMapping2(GridView.class);
        addMapping2(ClassChooserView.class);
        addMapping2(ObjectView.class);
        addMapping2(PropertyDrawView.class);
        addMapping2(RegularFilterView.class);
        addMapping2(RegularFilterGroupView.class);
        addMapping2(FunctionView.class);
    }

    // IDEA даёт ошибку при добавлении генериков,
    // хотя компилируется нормально, вставим такую затычку, чтобы не надо было везде кастить...
    protected void addMapping2(Class<? extends ServerCustomSerializable> clazz) {
        super.addMapping(clazz);
    }
}
