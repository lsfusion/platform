package lsfusion.server.physics.admin.drilldown;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.cases.CalcCase;

import java.util.ArrayList;
import java.util.List;

public class CaseUnionDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<CaseUnionProperty.Interface, CaseUnionProperty> {

    protected List<PropertyDrawEntity> propProperties;
    protected List<PropertyDrawEntity> whereProperties;
    protected PropertyDrawEntity implPropertyDraw;

    public CaseUnionDrillDownFormEntity(String canonicalName, LocalizedString caption, CaseUnionProperty property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();
        
        propProperties = new ArrayList<>();
        whereProperties = new ArrayList<>();

        ImList<CalcCase<UnionProperty.Interface>> cases = property.getCases();

        for (int i = 0; i < cases.size(); ++i) {
            CalcPropertyInterfaceImplement<CaseUnionProperty.Interface> intImpl = cases.get(i).implement;
            if (intImpl instanceof CalcPropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface>) intImpl;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !LM.getRecognizeGroup().hasChild(mapImplement.property)) {
                    if (mapImplement.property.isDrillFull()) {
                        propProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping, version)
                        );
                    }
                }
            }

            intImpl = cases.get(i).where;
            if (intImpl instanceof CalcPropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface>) intImpl;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !LM.getRecognizeGroup().hasChild(mapImplement.property)) {
                    if (mapImplement.property.isDrillFull()) {
                        whereProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping, version)
                        );
                    }
                }
            }
        }
        implPropertyDraw = addPropertyDraw(property, interfaceObjects, version);
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        valueContainer.add(design.get(implPropertyDraw), version);
        for (int i = propProperties.size()-1; i >= 0; i--) {
            ContainerView propsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.where}" + " " + (i + 1)), version);
            propsContainer.setAlignment(FlexAlignment.STRETCH);
            propsContainer.add(design.get(propProperties.get(i)), version);
            if (i < whereProperties.size()) // может быть else
                propsContainer.add(design.get(whereProperties.get(i)), version);
            design.mainContainer.addAfter(propsContainer, valueContainer, version);
        }
        return design;
    }
}
