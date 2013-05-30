package platform.server.form.entity.drilldown;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;

import java.util.ArrayList;
import java.util.List;

import static platform.server.logics.ServerResourceBundle.getString;

public class UnionDrillDownFormEntity<I extends PropertyInterface, P extends Property<I>> extends DrillDownFormEntity<UnionProperty.Interface, UnionProperty> {

    protected List<PropertyDrawEntity> operandProperties;
    protected PropertyDrawEntity implPropertyDraw;

    public UnionDrillDownFormEntity(String sID, String caption, UnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }

    @Override
    protected void setupDrillDownForm() {
        operandProperties = new ArrayList<PropertyDrawEntity>();

        ImCol<CalcPropertyInterfaceImplement<UnionProperty.Interface>> operands = property.getOperands();

        for (int i = 0; i < operands.size(); ++i) {
            CalcPropertyInterfaceImplement<UnionProperty.Interface> intImpl = operands.get(i);
            if (intImpl instanceof CalcPropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, UnionProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, UnionProperty.Interface>) intImpl;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !BL.LM.recognizeGroup.hasChild(mapImplement.property)) {
                    if (mapImplement.property.isFull()) {
                        operandProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping)
                        );
                    }
                }
            }
        }
        implPropertyDraw = addPropertyDraw(property, interfaceObjects);
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        valueContainer.add(design.get(implPropertyDraw));

        ContainerView operandsContainer = design.createContainer(getString("logics.property.drilldown.form.operands"));
        operandsContainer.constraints.fillHorizontal = 1;
        for (PropertyDrawEntity operandProperty : operandProperties) {
            operandsContainer.add(design.get(operandProperty));
        }
        design.mainContainer.addAfter(operandsContainer, valueContainer);
        return design;
    }
}
