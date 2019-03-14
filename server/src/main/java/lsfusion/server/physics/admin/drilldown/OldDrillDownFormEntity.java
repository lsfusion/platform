package lsfusion.server.physics.admin.drilldown;

import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.OldProperty;

public class OldDrillDownFormEntity extends DrillDownFormEntity<ClassPropertyInterface, OldProperty<ClassPropertyInterface>> {

    private PropertyDrawEntity propertyDraw;
    private PropertyDrawEntity oldPropertyDraw;

    public OldDrillDownFormEntity(String canonicalName, LocalizedString caption, OldProperty property, LogicsModule LM) {
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
        ContainerView oldValueContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.old.value}"), version);
        oldValueContainer.add(design.get(oldPropertyDraw), version);

        design.mainContainer.addAfter(oldValueContainer, valueContainer, version);

        return design;
    }
}
