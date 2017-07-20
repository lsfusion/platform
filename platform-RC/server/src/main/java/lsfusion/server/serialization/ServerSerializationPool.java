package lsfusion.server.serialization;

import lsfusion.base.serialization.SerializationPool;
import lsfusion.server.form.view.*;


public class ServerSerializationPool extends SerializationPool<ServerContext> {
    public ServerSerializationPool() {
        this(null);
    }

    public ServerSerializationPool(ServerContext context) {
        super(context);
        //порядок добавления должен соответствовать порядку в ClientSerializationPool

        addMapping2(FormView.class);
        addMapping2(ComponentView.class);
        addMapping2(ContainerView.class);
        addMapping2(GroupObjectView.class);
        addMapping2(TreeGroupView.class);
        addMapping2(ShowTypeView.class);
        addMapping2(GridView.class);
        addMapping2(ToolbarView.class);
        addMapping2(FilterView.class);
        addMapping2(ClassChooserView.class);
        addMapping2(ObjectView.class);
        addMapping2(PropertyDrawView.class);
        addMapping2(RegularFilterView.class);
        addMapping2(RegularFilterGroupView.class);
    }

    // IDEA даёт ошибку при добавлении генериков,
    // хотя компилируется нормально, вставим такую затычку, чтобы не надо было везде кастить...
    protected void addMapping2(Class<? extends ServerCustomSerializable> clazz) {
        super.addMapping(clazz);
    }
}
