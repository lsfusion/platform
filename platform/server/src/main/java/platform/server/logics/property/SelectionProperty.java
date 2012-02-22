package platform.server.logics.property;

import platform.interop.KeyStrokes;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.ServerResourceBundle;

public class SelectionProperty extends SessionDataProperty {

    ValueClass[] classes;

    public SelectionProperty(String sID, ValueClass[] classes) {
        super(sID, ServerResourceBundle.getString("logics.property.select"), classes, LogicalClass.instance);
        this.classes = classes;
    }

    @Override
    public String getCode() {
        StringBuilder result = new StringBuilder("selection.getLP(new ValueClass[]{");
        boolean first = true;
        for (ValueClass cls : classes) {
            if (first) {
                result.append(cls.getSID());
            } else {
                result.append(" ,").append(cls.getSID());
            }
        }
        result.append("})");
        return result.toString();
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
                new NotNullFilterEntity(entity.propertyObject),
                ServerResourceBundle.getString("logics.property.selected"),
                KeyStrokes.getSelectionFilterKeyStroke()), false);
        form.addRegularFilterGroup(filterGroup);

        entity.getToDraw(form).propertyHighlight = entity.propertyObject;
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.editKey = KeyStrokes.getSelectionPropertyKeyStroke();
        propertyView.editOnSingleClick = true;
    }
}
