package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.util.ArrayList;
import java.util.List;

public class JoinDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<JoinProperty.Interface, JoinProperty<I>> {

    private List<PropertyDrawEntity> detailsProperties;
    private PropertyDrawEntity implPropertyDraw;

    public JoinDrillDownFormEntity(String canonicalName, LocalizedString caption, JoinProperty<I> property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        CalcProperty<I> implProperty = property.implement.property;
        if(implProperty.isDrillFull()) {
            detailsProperties = new ArrayList<>();
            Version version = LM.getVersion();

            ImMap<I, CalcPropertyInterfaceImplement<JoinProperty.Interface>> implMapping = property.implement.mapping;
            ImMap<I, ValueClass> implClasses = implProperty.getInterfaceClasses(ClassType.drillDownPolicy);
    
            MRevMap<I, ObjectEntity> mImplObjects = MapFact.mRevMap();
            MAddSet<ObjectEntity> usedObjects = SetFact.mAddSet();
    
            for (int i = 0; i < implMapping.size(); ++i) {
                I iFace = implMapping.getKey(i);
                CalcPropertyInterfaceImplement<JoinProperty.Interface> intImpl = implMapping.getValue(i);
                ObjectEntity innerObject = null;
                if (intImpl instanceof JoinProperty.Interface) {
                    JoinProperty.Interface intImplement = (JoinProperty.Interface) intImpl;
                    innerObject = interfaceObjects.get(intImplement);
                } 
                if(innerObject == null || usedObjects.add(innerObject)) {
                    //добавляем дополнительный объект, если на входе - свойство
                    innerObject  = addSingleGroupObject(implClasses.get(iFace), version);
                    innerObject.groupTo.setPanelClassView();
    
                    PropertyInterface innerInterface = new PropertyInterface();
                    CalcPropertyRevImplement filterProp = DerivedProperty.createCompare(intImpl, innerInterface, Compare.EQUALS).mapRevImplement(MapFact.addRevExcl(interfaceObjects, innerInterface, innerObject));
                    addFixedFilter(new FilterEntity(addPropertyObject(filterProp)), version);
                    
                    if(intImpl instanceof CalcPropertyMapImplement) {
                        CalcPropertyMapImplement<PropertyInterface, JoinProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, JoinProperty.Interface>) intImpl;
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
