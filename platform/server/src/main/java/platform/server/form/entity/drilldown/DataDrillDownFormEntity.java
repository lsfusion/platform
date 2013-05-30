package platform.server.form.entity.drilldown;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.PropertyInterface;

import static platform.server.logics.ServerResourceBundle.getString;

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
