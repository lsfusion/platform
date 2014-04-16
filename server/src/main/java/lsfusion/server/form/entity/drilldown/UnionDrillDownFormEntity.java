package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class UnionDrillDownFormEntity<I extends PropertyInterface, P extends Property<I>> extends DrillDownFormEntity<UnionProperty.Interface, UnionProperty> {

    protected List<PropertyDrawEntity> operandProperties;
    protected PropertyDrawEntity implPropertyDraw;

    public UnionDrillDownFormEntity(String sID, String caption, UnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();
        
        operandProperties = new ArrayList<PropertyDrawEntity>();

        ImCol<CalcPropertyInterfaceImplement<UnionProperty.Interface>> operands = property.getOperands();

        for (int i = 0; i < operands.size(); ++i) {
            CalcPropertyInterfaceImplement<UnionProperty.Interface> intImpl = operands.get(i);
            if (intImpl instanceof CalcPropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, UnionProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, UnionProperty.Interface>) intImpl;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !LM.recognizeGroup.hasChild(mapImplement.property)) {
                    if (mapImplement.property.isFull()) {
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

        ContainerView operandsContainer = design.createContainer(getString("logics.property.drilldown.form.operands"));
        operandsContainer.setAlignment(FlexAlignment.STRETCH);
        for (PropertyDrawEntity operandProperty : operandProperties) {
            operandsContainer.add(design.get(operandProperty), version);
        }
        design.mainContainer.addAfter(operandsContainer, valueContainer, version);
        return design;
    }
}
