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
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.PropertyInterface;

public class DrillDownFormEntity<I extends PropertyInterface, P extends CalcProperty<I>> extends FormEntity {
    protected final P property;
    protected final LogicsModule LM;

    public final ImMap<I, ObjectEntity> interfaceObjects;
    public final ObjectEntity[] paramObjects;

    public DrillDownFormEntity(String canonicalName, LocalizedString caption, P property, LogicsModule LM) {
        super(canonicalName, caption, LM.getVersion());

        this.property = property;
        this.LM = LM;
        Version version = LM.getVersion();

        paramObjects = new ObjectEntity[property.interfaces.size()];
        MMap<I, ObjectEntity> interfaceObjects = MapFact.mMap(MapFact.<I, ObjectEntity>override());

        ImMap<I,ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.drillDownPolicy);
        int i = 0;
        for (I pi : property.getReflectionOrderInterfaces()) {
            ObjectEntity paramObject  = addSingleGroupObject(interfaceClasses.get(pi), version, LM.recognizeGroup, true);
            addPropertyDraw(LM.getObjValueProp(this, paramObject), version, paramObject);
            paramObject.groupTo.setSingleClassView(ClassViewType.PANEL);

            interfaceObjects.add(pi, paramObject);
            paramObjects[i++] = paramObject;
        }

        this.interfaceObjects = interfaceObjects.immutable();

        setupDrillDownForm();

        setNFEditType(PropertyEditType.READONLY, version);

        finalizeInit(version); // не красиво конечно, но иначе по хорошему надо во все setEditType version'ы вставлять
    }

    protected void setupDrillDownForm() {
    }

    protected ContainerView valueContainer;
    protected ContainerView paramsContainer;
    protected ContainerView detailsContainer;

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        paramsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.params}"), version);
        paramsContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addFirst(paramsContainer, version);
        for (ObjectEntity obj : paramObjects) {
            paramsContainer.add(design.getGroupObjectContainer(obj.groupTo), version);
        }

        valueContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.value}"), version);
        valueContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addAfter(valueContainer, paramsContainer, version);

        detailsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.details}"), version);
        detailsContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addAfter(detailsContainer, valueContainer, version);

        return design;
    }

    @Override
    public boolean needsToBeSynchronized() {
        return false;
    }
}
