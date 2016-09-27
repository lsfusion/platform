package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.CompareFilterEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;

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
    
            MMap<I, ObjectEntity> mImplObjects = MapFact.mMap(MapFact.<I, ObjectEntity>override());
    
            for (int i = 0; i < implMapping.size(); ++i) {
                I iFace = implMapping.getKey(i);
                CalcPropertyInterfaceImplement<JoinProperty.Interface> intImpl = implMapping.getValue(i);
                if (intImpl instanceof CalcPropertyMapImplement) {
                    //добавляем дополнительный объект, если на входе - свойство
                    ObjectEntity innerObject  = addSingleGroupObject(implClasses.get(iFace), version);
                    innerObject.groupTo.setSingleClassView(ClassViewType.PANEL);
                    mImplObjects.add(iFace, innerObject);
    
                    //добавляем фильтр для этого объекта и соотв. свойства
                    CalcPropertyMapImplement<PropertyInterface, JoinProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, JoinProperty.Interface>) intImpl;
                    ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;
                    addFixedFilter(new CompareFilterEntity(addPropertyObject(mapImplement.property, mapImplMapping), Compare.EQUALS, innerObject), version);
    
                    //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                    if (mapImplMapping.size() != 1 || !LM.recognizeGroup.hasNFChild(mapImplement.property, version)) {
                        if (mapImplement.property.isDrillFull()) {
                            detailsProperties.add(
                                    addPropertyDraw(mapImplement.property, mapImplMapping, version)
                            );
                        }
                    }
                } else {
                    JoinProperty.Interface intImplement = (JoinProperty.Interface) intImpl;
                    mImplObjects.add(iFace, interfaceObjects.get(intImplement));
                }
            }

            ImMap<I, ObjectEntity> implObjects = mImplObjects.immutable();
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
                if (detailPropertyView.getCaption().isEmpty()) {
                    detailPropertyView.caption = LocalizedString.create(detailProperty.propertyObject.property.getName());
                }
                detailsContainer.add(detailPropertyView, version);
            }

            valueContainer.add(design.get(implPropertyDraw), version);
        }

        return design;
    }
}
