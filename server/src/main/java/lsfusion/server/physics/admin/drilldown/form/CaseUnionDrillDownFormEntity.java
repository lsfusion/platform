package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.ArrayList;
import java.util.List;

public class CaseUnionDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<CaseUnionProperty.Interface, CaseUnionProperty> {

    protected List<PropertyDrawEntity> propProperties;
    protected List<PropertyDrawEntity> whereProperties;
    protected PropertyDrawEntity implPropertyDraw;

    public CaseUnionDrillDownFormEntity(LocalizedString caption, CaseUnionProperty property, BaseLogicsModule LM) {
        super(caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        propProperties = new ArrayList<>();
        whereProperties = new ArrayList<>();

        ImList<CalcCase<UnionProperty.Interface>> cases = property.getCases();

        for (int i = 0; i < cases.size(); ++i) {
            PropertyInterfaceImplement<CaseUnionProperty.Interface> intImpl = cases.get(i).implement;
            if (intImpl instanceof PropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                PropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface> mapImplement = (PropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface>) intImpl;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || isNotId(mapImplement)) {
                    if (mapImplement.property.isDrillFull()) {
                        propProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping)
                        );
                    }
                }
            }

            intImpl = cases.get(i).where;
            if (intImpl instanceof PropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                PropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface> mapImplement = (PropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface>) intImpl;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || isNotId(mapImplement)) {
                    if (mapImplement.property.isDrillFull()) {
                        whereProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping)
                        );
                    }
                }
            }
        }
        implPropertyDraw = addPropertyDraw(property, interfaceObjects);
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
