package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class DrillDownFormEntity<I extends PropertyInterface, P extends Property<I>> extends FormEntity {
    protected final P property;
    protected final LogicsModule LM;

    public final ImMap<I, ObjectEntity> interfaceObjects;
    public final ObjectEntity[] paramObjects;

    public DrillDownFormEntity(String sID, String caption, P property, LogicsModule LM) {
        super(sID, caption, LM.getVersion());

        this.property = property;
        this.LM = LM;
        Version version = LM.getVersion();

        paramObjects = new ObjectEntity[property.interfaces.size()];
        MMap<I, ObjectEntity> interfaceObjects = MapFact.mMap(MapFact.<I, ObjectEntity>override());

        ImMap<I, ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.ASSERTFULL);
        int i = 0;
        for (I pi : property.getOrderInterfaces()) {
            ObjectEntity paramObject  = addSingleGroupObject(interfaceClasses.get(pi), version, LM.baseLM.objectValue, LM.recognizeGroup, true);
            paramObject.groupTo.setSingleClassView(ClassViewType.PANEL);

            interfaceObjects.add(pi, paramObject);
            paramObjects[i++] = paramObject;
        }

        this.interfaceObjects = interfaceObjects.immutable();

        setupDrillDownForm();

        finalizeInit(version); // не красиво конечно, но иначе по хорошему надо во все setEditType version'ы вставлять

        setEditType(PropertyEditType.READONLY);
    }

    protected void setupDrillDownForm() {
    }

    protected ContainerView valueContainer;
    protected ContainerView paramsContainer;
    protected ContainerView detailsContainer;

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        paramsContainer = design.createContainer(getString("logics.property.drilldown.form.params"));
        paramsContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addFirst(paramsContainer, version);
        for (ObjectEntity obj : paramObjects) {
            paramsContainer.add(design.getGroupObjectContainer(obj.groupTo), version);
        }

        valueContainer = design.createContainer(getString("logics.property.drilldown.form.value"));
        valueContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addAfter(valueContainer, paramsContainer, version);

        detailsContainer = design.createContainer(getString("logics.property.drilldown.form.details"));
        detailsContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addAfter(detailsContainer, valueContainer, version);

        return design;
    }
}
