package lsfusion.server.logics.form.struct.drilldown;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.PropertyInterface;

public class DrillDownFormEntity<I extends PropertyInterface, P extends CalcProperty<I>> extends FormEntity {
    protected final P property;
    protected final LogicsModule LM;

    public final ImRevMap<I, ObjectEntity> interfaceObjects;
    public final ImOrderSet<ObjectEntity> paramObjects;

    public DrillDownFormEntity(String canonicalName, LocalizedString caption, P property, LogicsModule LM) {
        super(canonicalName, caption, LM.getVersion());

        this.property = property;
        this.LM = LM;
        Version version = LM.getVersion();

        MOrderExclSet<ObjectEntity> mParamObjects = SetFact.mOrderExclSet(property.interfaces.size());
        MRevMap<I, ObjectEntity> mInterfaceObjects = MapFact.mRevMap();

        ImMap<I,ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.drillDownPolicy);
        int i = 0;
        for (I pi : property.getReflectionOrderInterfaces()) {
            ObjectEntity paramObject = addSingleGroupObject(interfaceClasses.get(pi), version);
            addPropertyDraw(paramObject, version, LM.getRecognizeGroup());
            addPropertyDraw(LM.getObjValueProp(this, paramObject), version, paramObject);
            paramObject.groupTo.setPanelClassView();

            mInterfaceObjects.revAdd(pi, paramObject);
            mParamObjects.exclAdd(paramObject);
        }

        this.interfaceObjects = mInterfaceObjects.immutableRev();
        this.paramObjects = mParamObjects.immutableOrder();

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
            paramsContainer.add(design.getBoxContainer(obj.groupTo), version);
        }

        valueContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.value}"), version);
        valueContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addAfter(valueContainer, paramsContainer, version);

        detailsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.details}"), version);
        detailsContainer.setFlex(1.0);
        detailsContainer.setAlignment(FlexAlignment.STRETCH);
        design.mainContainer.addAfter(detailsContainer, valueContainer, version);

        return design;
    }

    @Override
    public boolean needsToBeSynchronized() {
        return false;
    }
}
