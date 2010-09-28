package platform.server.logics.property;

import platform.server.logics.SessionDataProperty;
import platform.server.classes.ValueClass;
import platform.server.classes.LogicalClass;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

public class SelectionProperty extends SessionDataProperty {

    public SelectionProperty(String sID, ValueClass[] classes) {
        super(sID, "Отметить", classes, LogicalClass.instance);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
        RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(form.genID());
        filterGroup.addFilter(new RegularFilterEntity(form.genID(),
                                                      new NotNullFilterEntity(entity.propertyObject),
                                                      "Отмеченные",
                                                      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK)), false);
        form.addRegularFilterGroup(filterGroup);

        entity.toDraw.propertyHighlight = entity.propertyObject;
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        PropertyDrawView selectionPropView = view.get(entity);
        selectionPropView.editKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK);
    }
}
