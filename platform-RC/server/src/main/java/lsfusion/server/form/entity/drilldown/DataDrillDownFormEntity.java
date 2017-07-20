package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.PropertyInterface;

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

        CalcPropertyMapImplement<?, ClassPropertyInterface> where = (CalcPropertyMapImplement<?, ClassPropertyInterface>) property.event.where; //h
        ImRevMap<PropertyInterface, ClassPropertyInterface> whereMapping = (ImRevMap<PropertyInterface, ClassPropertyInterface>) where.mapping;
        MMap<PropertyInterface, ObjectEntity> mapping = MapFact.mMap(MapFact.<PropertyInterface, ObjectEntity>override());
        for (PropertyInterface i : whereMapping.keys()) {
                mapping.add(i, interfaceObjects.get(whereMapping.get(i)));
        }
        wherePropertyDraw = addPropertyDraw(where.property, mapping.immutable(), version);

        CalcPropertyMapImplement<?, ClassPropertyInterface> writeFrom = (CalcPropertyMapImplement<?, ClassPropertyInterface>) property.event.writeFrom; //g
        ImRevMap<PropertyInterface, ClassPropertyInterface> writeFromMapping = (ImRevMap<PropertyInterface, ClassPropertyInterface>) writeFrom.mapping;
        mapping = MapFact.mMap(MapFact.<PropertyInterface, ObjectEntity>override());
        for (PropertyInterface i : writeFromMapping.keys()) {
                mapping.add(i, interfaceObjects.get(writeFromMapping.get(i)));
        }
        writeFromPropertyDraw = addPropertyDraw(writeFrom.property, mapping.immutable(), version);
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
