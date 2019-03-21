package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.ArrayList;
import java.util.List;

public class UnionDrillDownFormEntity<I extends PropertyInterface, P extends Property<I>> extends DrillDownFormEntity<UnionProperty.Interface, UnionProperty> {

    protected List<PropertyDrawEntity> operandProperties;
    protected PropertyDrawEntity implPropertyDraw;

    public UnionDrillDownFormEntity(String canonicalName, LocalizedString caption, UnionProperty property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();
        
        operandProperties = new ArrayList<>();

        ImCol<PropertyInterfaceImplement<UnionProperty.Interface>> operands = property.getOperands();

        for (int i = 0; i < operands.size(); ++i) {
            PropertyInterfaceImplement<UnionProperty.Interface> intImpl = operands.get(i);
            if (intImpl instanceof PropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                PropertyMapImplement<PropertyInterface, UnionProperty.Interface> mapImplement = (PropertyMapImplement<PropertyInterface, UnionProperty.Interface>) intImpl;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !LM.getRecognizeGroup().hasChild(mapImplement.property)) {
                    if (mapImplement.property.isDrillFull()) {
                        operandProperties.add(
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

        ContainerView operandsContainer = design.createContainer(LocalizedString.create("{logics.property.drilldown.form.operands}"), version);
        operandsContainer.setAlignment(FlexAlignment.STRETCH);
        for (PropertyDrawEntity operandProperty : operandProperties) {
            operandsContainer.add(design.get(operandProperty), version);
        }
        design.mainContainer.addAfter(operandsContainer, valueContainer, version);
        return design;
    }
}
