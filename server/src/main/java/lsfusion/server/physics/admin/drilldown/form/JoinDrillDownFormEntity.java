package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.ArrayList;
import java.util.List;

public class JoinDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<JoinProperty.Interface, JoinProperty<I>> {

    private List<PropertyDrawEntity> detailsProperties;
    private PropertyDrawEntity implPropertyDraw;

    public JoinDrillDownFormEntity(LocalizedString caption, JoinProperty<I> property, LogicsModule LM) {
        super(caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Property<I> implProperty = property.implement.property;
        if(implProperty.isDrillFull()) {
            detailsProperties = new ArrayList<>();
            Version version = LM.getVersion();

            ImMap<I, PropertyInterfaceImplement<JoinProperty.Interface>> implMapping = property.implement.mapping;
            ImMap<I, ValueClass> implClasses = implProperty.getInterfaceClasses(ClassType.drillDownPolicy);
    
            MRevMap<I, ObjectEntity> mImplObjects = MapFact.mRevMap();
            MAddSet<ObjectEntity> usedObjects = SetFact.mAddSet();
    
            for (int i = 0; i < implMapping.size(); ++i) {
                I iFace = implMapping.getKey(i);
                PropertyInterfaceImplement<JoinProperty.Interface> intImpl = implMapping.getValue(i);
                ObjectEntity innerObject = null;
                if (intImpl instanceof JoinProperty.Interface) {
                    JoinProperty.Interface intImplement = (JoinProperty.Interface) intImpl;
                    innerObject = interfaceObjects.get(intImplement);
                } 
                if(innerObject == null || usedObjects.add(innerObject)) {
                    //добавляем дополнительный объект, если на входе - свойство
                    innerObject  = addSingleGroupObject(implClasses.get(iFace), version);
                    innerObject.groupTo.setViewTypePanel();
    
                    PropertyInterface innerInterface = new PropertyInterface();
                    PropertyRevImplement filterProp = PropertyFact.createCompare(intImpl, innerInterface, Compare.EQUALS).mapRevImplement(MapFact.addRevExcl(interfaceObjects, innerInterface, innerObject));
                    addFixedFilter(new FilterEntity(addPropertyObject(filterProp)), version);
                    
                    if(intImpl instanceof PropertyMapImplement) {
                        PropertyMapImplement<PropertyInterface, JoinProperty.Interface> mapImplement = (PropertyMapImplement<PropertyInterface, JoinProperty.Interface>) intImpl;
                        ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(interfaceObjects).mapping;
                        //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                        if (mapImplMapping.size() != 1 || !LM.getRecognizeGroup().hasNFChild(mapImplement.property, version)) {
                            if (mapImplement.property.isDrillFull()) {
                                detailsProperties.add(addPropertyDraw(mapImplement.property, mapImplMapping, version));
                            }
                        }
                    }
                }
                mImplObjects.revAdd(iFace, innerObject);
            }

            ImRevMap<I, ObjectEntity> implObjects = mImplObjects.immutableRev();
            implPropertyDraw = addPropertyDraw(implProperty, implObjects, version);
        }
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        ContainerView extraParamsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.inner.params}"), version);
        design.mainContainer.addAfter(extraParamsContainer, valueContainer, version);

        if(implPropertyDraw != null) {
            for (PropertyDrawEntity detailProperty : detailsProperties) {
                PropertyDrawView detailPropertyView = design.get(detailProperty);
                detailsContainer.add(detailPropertyView, version);
            }

            valueContainer.add(design.get(implPropertyDraw), version);
        }

        return design;
    }
}
