package lsfusion.server.form.entity.drilldown;

import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.OldProperty;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class OldDrillDownFormEntity extends DrillDownFormEntity<ClassPropertyInterface, OldProperty<ClassPropertyInterface>> {

    private PropertyDrawEntity propertyDraw;
    private PropertyDrawEntity oldPropertyDraw;

    public OldDrillDownFormEntity(String canonicalName, String caption, OldProperty property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();
        propertyDraw = addPropertyDraw(property, interfaceObjects, version);
        oldPropertyDraw = addPropertyDraw(property.property, interfaceObjects, version);
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        valueContainer.add(design.get(propertyDraw), version);
        ContainerView oldValueContainer = design.createContainer(getString("logics.property.drilldown.form.old.value"), version);
        oldValueContainer.add(design.get(oldPropertyDraw), version);

        design.mainContainer.addAfter(oldValueContainer, valueContainer, version);

        return design;
    }
}
