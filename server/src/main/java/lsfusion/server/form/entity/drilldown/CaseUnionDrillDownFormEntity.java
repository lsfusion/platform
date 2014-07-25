package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CaseUnionDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<CaseUnionProperty.Interface, CaseUnionProperty> {

    protected List<PropertyDrawEntity> propProperties;
    protected List<PropertyDrawEntity> whereProperties;
    protected PropertyDrawEntity implPropertyDraw;

    public CaseUnionDrillDownFormEntity(String canonicalName, String caption, CaseUnionProperty property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();
        
        propProperties = new ArrayList<PropertyDrawEntity>();
        whereProperties = new ArrayList<PropertyDrawEntity>();

        ImList<CaseUnionProperty.Case> cases = property.getCases();

        for (int i = 0; i < cases.size(); ++i) {
            CalcPropertyInterfaceImplement<CaseUnionProperty.Interface> intImpl = cases.get(i).property;
            if (intImpl instanceof CalcPropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface>) intImpl;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !LM.recognizeGroup.hasChild(mapImplement.property)) {
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
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !LM.recognizeGroup.hasChild(mapImplement.property)) {
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
            ContainerView propsContainer = design.createContainer(getString("logics.property.drilldown.form.where") + " " + (i + 1));
            propsContainer.setAlignment(FlexAlignment.STRETCH);
            propsContainer.add(design.get(propProperties.get(i)), version);
            propsContainer.add(design.get(whereProperties.get(i)), version);
            design.mainContainer.addAfter(propsContainer, valueContainer, version);
        }
        return design;
    }
}
