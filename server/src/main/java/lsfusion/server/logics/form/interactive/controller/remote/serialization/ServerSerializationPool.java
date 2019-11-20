package lsfusion.server.logics.form.interactive.controller.remote.serialization;

import lsfusion.interop.form.remote.serialization.SerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.*;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;


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
        addMapping2(GridView.class);
        addMapping2(ToolbarView.class);
        addMapping2(FilterView.class);
        addMapping2(CalculationsView.class);
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
