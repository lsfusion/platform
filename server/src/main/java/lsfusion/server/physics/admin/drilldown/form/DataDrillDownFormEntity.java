package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class DataDrillDownFormEntity extends DrillDownFormEntity<ClassPropertyInterface, DataProperty> {

    private PropertyDrawEntity implPropertyDraw;
    private PropertyDrawEntity wherePropertyDraw;
    private PropertyDrawEntity writeFromPropertyDraw;

    public DataDrillDownFormEntity(String canonicalName, LocalizedString caption, DataProperty property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();

        implPropertyDraw = addPropertyDraw(property, interfaceObjects, version);

        PropertyMapImplement<PropertyInterface, ClassPropertyInterface> where = (PropertyMapImplement<PropertyInterface, ClassPropertyInterface>) property.event.where; //h
        ImRevMap<PropertyInterface, ClassPropertyInterface> whereMapping = where.mapping;
        wherePropertyDraw = addPropertyDraw(where.property, whereMapping.join(interfaceObjects), version);

        PropertyMapImplement<PropertyInterface, ClassPropertyInterface> writeFrom = (PropertyMapImplement<PropertyInterface, ClassPropertyInterface>) property.event.writeFrom; //g
        ImRevMap<PropertyInterface, ClassPropertyInterface> writeFromMapping = writeFrom.mapping;
        writeFromPropertyDraw = addPropertyDraw(writeFrom.property, writeFromMapping.join(interfaceObjects), version);
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);
        valueContainer.add(design.get(implPropertyDraw), version);

        ContainerView whereParamsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.where.params}"), version);
        whereParamsContainer.add(design.get(wherePropertyDraw), version);
        ContainerView expressionParamsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.expr.params}"), version);
        expressionParamsContainer.add(design.get(writeFromPropertyDraw), version);

        design.mainContainer.addAfter(whereParamsContainer, valueContainer, version);
        design.mainContainer.addAfter(expressionParamsContainer, whereParamsContainer, version);

        return design;
    }
}
