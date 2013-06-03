package lsfusion.server.form.entity.drilldown;

import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.OldProperty;

import static lsfusion.server.logics.ServerResourceBundle.getString;

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
