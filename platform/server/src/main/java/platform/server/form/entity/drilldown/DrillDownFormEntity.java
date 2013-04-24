package platform.server.form.entity.drilldown;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import static platform.server.logics.ServerResourceBundle.getString;

public class DrillDownFormEntity<I extends PropertyInterface, P extends Property<I>> extends FormEntity {
    protected final P property;
    protected final BusinessLogics BL;

    public final ImMap<I, ObjectEntity> interfaceObjects;
    public final ObjectEntity[] paramObjects;

    public DrillDownFormEntity(String sID, String caption, P property, BusinessLogics BL) {
        super(sID, caption);

        this.property = property;
        this.BL = BL;

        paramObjects = new ObjectEntity[property.interfaces.size()];
        MMap<I, ObjectEntity> interfaceObjects = MapFact.mMap(MapFact.<I, ObjectEntity>override());

        ImMap<I, ValueClass> interfaceClasses = property.getInterfaceClasses();
        int i = 0;
        for (I pi : property.interfaces) {
            ObjectEntity paramObject  = addSingleGroupObject(interfaceClasses.get(pi), BL.LM.objectValue, BL.LM.recognizeGroup, true);
            paramObject.groupTo.setSingleClassView(ClassViewType.PANEL);

            interfaceObjects.add(pi, paramObject);
            paramObjects[i++] = paramObject;
        }

        this.interfaceObjects = interfaceObjects.immutable();

        setupDrillDownForm();

        setEditType(PropertyEditType.READONLY);
    }

    protected void setupDrillDownForm() {
    }

    protected ContainerView valueContainer;
    protected ContainerView paramsContainer;
    protected ContainerView detailsContainer;

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        paramsContainer = design.createContainer(getString("logics.property.drilldown.form.params"));
        paramsContainer.constraints.fillHorizontal = 1;
        design.mainContainer.addFirst(paramsContainer);
        for (ObjectEntity obj : paramObjects) {
            paramsContainer.add(design.getGroupObjectContainer(obj.groupTo));
        }

        valueContainer = design.createContainer(getString("logics.property.drilldown.form.value"));
        valueContainer.constraints.fillHorizontal = 1;
        design.mainContainer.addAfter(valueContainer, paramsContainer);

        detailsContainer = design.createContainer(getString("logics.property.drilldown.form.details"));
        detailsContainer.constraints.fillHorizontal = 1;
        design.mainContainer.addAfter(detailsContainer, valueContainer);

        return design;
    }
}
