package platform.server.logics.property;

import platform.interop.KeyStrokes;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.ServerResourceBundle;

import java.util.Collection;

public class SelectionProperty extends SessionDataProperty {

    ValueClass[] classes;
    BaseLogicsModule LM;

    public SelectionProperty(String sID, ValueClass[] classes, BaseLogicsModule LM) {
        super(sID, ServerResourceBundle.getString("logics.property.select"), classes, LogicalClass.instance);
        this.classes = classes;
        this.LM = LM;
    }

    @Override
    public boolean isField() {
        return true;
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(form.genID());
        filterGroup.addFilter(new RegularFilterEntity(form.genID(),
                new NotNullFilterEntity((CalcPropertyObjectEntity) entity.propertyObject),
                ServerResourceBundle.getString("logics.property.selected"),
                KeyStrokes.getSelectionFilterKeyStroke()), false);
        form.addRegularFilterGroup(filterGroup);

        Collection<ObjectEntity> objects = entity.propertyObject.getObjectInstances();
        Object[] params = new Object[objects.size() + 2];
        params[0] = LM.baseLM.defaultOverrideBackgroundColor;
        params[1] = LM.getLP(entity.propertyObject.property.getSID());
        for (int i = 0; i < objects.size(); i++) {
            params[i + 2] = i + 1;
        }
        entity.getToDraw(form).propertyBackground = form.addPropertyObject(LM.addJProp(LM.and1, params), objects.toArray(new ObjectEntity[objects.size()]));
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.editKey = KeyStrokes.getSelectionPropertyKeyStroke();
        propertyView.editOnSingleClick = true;
    }
}
