package platform.server.form.entity.drilldown;

import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.OldProperty;

import static platform.server.logics.ServerResourceBundle.getString;

public class OldDrillDownFormEntity extends DrillDownFormEntity<ClassPropertyInterface, OldProperty<ClassPropertyInterface>> {

    private PropertyDrawEntity propertyDraw;
    private PropertyDrawEntity oldPropertyDraw;

    public OldDrillDownFormEntity(String sID, String caption, OldProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }

    @Override
    protected void setupDrillDownForm() {
        propertyDraw = addPropertyDraw(property, interfaceObjects);
        oldPropertyDraw = addPropertyDraw(property.property, interfaceObjects);
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        valueContainer.add(design.get(propertyDraw));
        ContainerView oldValueContainer = design.createContainer(getString("logics.property.drilldown.form.old.value"));
        oldValueContainer.add(design.get(oldPropertyDraw));

        design.mainContainer.addAfter(oldValueContainer, valueContainer);

        return design;
    }
}
