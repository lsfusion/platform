package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.PropertyInterface;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class DataDrillDownFormEntity extends DrillDownFormEntity<ClassPropertyInterface, DataProperty> {

    private PropertyDrawEntity implPropertyDraw;
    private PropertyDrawEntity wherePropertyDraw;
    private PropertyDrawEntity writeFromPropertyDraw;

    public DataDrillDownFormEntity(String sID, String caption, DataProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }

    @Override
    protected void setupDrillDownForm() {
        implPropertyDraw = addPropertyDraw(property, interfaceObjects);

        CalcPropertyMapImplement<?, ClassPropertyInterface> where = (CalcPropertyMapImplement<?, ClassPropertyInterface>) property.event.where; //h
        ImRevMap<PropertyInterface, ClassPropertyInterface> whereMapping = (ImRevMap<PropertyInterface, ClassPropertyInterface>) where.mapping;
        MMap<PropertyInterface, ObjectEntity> mapping = MapFact.mMap(MapFact.<PropertyInterface, ObjectEntity>override());
        for (PropertyInterface i : whereMapping.keys()) {
                mapping.add(i, interfaceObjects.get(whereMapping.get(i)));
        }
        wherePropertyDraw = addPropertyDraw(where.property, mapping.immutable());

        CalcPropertyMapImplement<?, ClassPropertyInterface> writeFrom = (CalcPropertyMapImplement<?, ClassPropertyInterface>) property.event.writeFrom; //g
        ImRevMap<PropertyInterface, ClassPropertyInterface> writeFromMapping = (ImRevMap<PropertyInterface, ClassPropertyInterface>) writeFrom.mapping;
        mapping = MapFact.mMap(MapFact.<PropertyInterface, ObjectEntity>override());
        for (PropertyInterface i : writeFromMapping.keys()) {
                mapping.add(i, interfaceObjects.get(writeFromMapping.get(i)));
        }
        writeFromPropertyDraw = addPropertyDraw(writeFrom.property, mapping.immutable());
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
        valueContainer.add(design.get(implPropertyDraw));

        ContainerView whereParamsContainer = design.createContainer(getString("logics.property.drilldown.form.where.params"));
        whereParamsContainer.add(design.get(wherePropertyDraw));
        ContainerView expressionParamsContainer = design.createContainer(getString("logics.property.drilldown.form.expr.params"));
        expressionParamsContainer.add(design.get(writeFromPropertyDraw));

        design.mainContainer.addAfter(whereParamsContainer, valueContainer);
        design.mainContainer.addAfter(expressionParamsContainer, whereParamsContainer);

        return design;
    }
}
