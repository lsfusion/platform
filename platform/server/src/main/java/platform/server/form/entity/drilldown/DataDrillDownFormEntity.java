package platform.server.form.entity.drilldown;

import platform.base.col.MapFact;
import platform.base.col.implementations.simple.SingletonRevMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.interop.ClassViewType;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;

import java.util.ArrayList;
import java.util.List;

import static platform.base.BaseUtils.isRedundantString;
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

        CalcProperty<?> where = property.event.getWhere(); //h
        ImRevMap whereMapping = where.getImplement().mapping;
        MMap<ClassPropertyInterface, ObjectEntity> whereObjects = MapFact.mMap(MapFact.<ClassPropertyInterface, ObjectEntity>override());
        ImMap<ClassPropertyInterface, ValueClass> whereClasses = (ImMap<ClassPropertyInterface, ValueClass>) where.getInterfaceClasses();
        for (int i = 0; i < whereMapping.size(); ++i) {
            if (whereMapping.getKey(i) instanceof ClassPropertyInterface) {
                ClassPropertyInterface iFace = (ClassPropertyInterface) whereMapping.getKey(i);
                ObjectEntity innerObject = addSingleGroupObject(whereClasses.get(iFace));
                innerObject.groupTo.setSingleClassView(ClassViewType.PANEL);
                whereObjects.add(iFace, innerObject);
            }
        }
        wherePropertyDraw = addPropertyDraw(where, whereObjects.immutable());

        CalcProperty<?> writeFrom = property.event.getWriteFrom(); //g
        ImRevMap writeFromMapping = writeFrom.getImplement().mapping;
        MMap<ClassPropertyInterface, ObjectEntity> writeFromObjects = MapFact.mMap(MapFact.<ClassPropertyInterface, ObjectEntity>override());
        ImMap<ClassPropertyInterface, ValueClass> writeFromClasses = (ImMap<ClassPropertyInterface, ValueClass>) writeFrom.getInterfaceClasses();
        for (int i = 0; i < writeFromMapping.size(); ++i) {
            if (writeFromMapping.getKey(i) instanceof ClassPropertyInterface) {
                ClassPropertyInterface iFace = (ClassPropertyInterface) writeFromMapping.getKey(i);
                ObjectEntity innerObject = addSingleGroupObject(writeFromClasses.get(iFace));
                innerObject.groupTo.setSingleClassView(ClassViewType.PANEL);
                writeFromObjects.add(iFace, innerObject);
            }
        }
        writeFromPropertyDraw = addPropertyDraw(writeFrom, writeFromObjects.immutable());
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
