package platform.server.form.entity.drilldown;

import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ChangedProperty;
import platform.server.logics.property.ClassPropertyInterface;

import static platform.server.logics.ServerResourceBundle.getString;

public class ChangedDrillDownFormEntity extends DrillDownFormEntity<ClassPropertyInterface, ChangedProperty<ClassPropertyInterface>> {

    private PropertyDrawEntity propertyDraw;
    private PropertyDrawEntity newPropertyDraw;
    private PropertyDrawEntity oldPropertyDraw;

    public ChangedDrillDownFormEntity(String sID, String caption, ChangedProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }

    @Override
    protected void setupDrillDownForm() {
        propertyDraw = addPropertyDraw(property, interfaceObjects);
        newPropertyDraw = addPropertyDraw(property.property, interfaceObjects);
        oldPropertyDraw = addPropertyDraw(property.property.getOld(), interfaceObjects);
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        valueContainer.add(design.get(propertyDraw));
        ContainerView newValueContainer = design.createContainer(getString("logics.property.drilldown.form.new.value"));
        newValueContainer.add(design.get(newPropertyDraw));
        ContainerView oldValueContainer = design.createContainer(getString("logics.property.drilldown.form.old.value"));
        oldValueContainer.add(design.get(oldPropertyDraw));

        design.mainContainer.addAfter(newValueContainer, valueContainer);
        design.mainContainer.addAfter(oldValueContainer, newValueContainer);

        return design;
    }
}
