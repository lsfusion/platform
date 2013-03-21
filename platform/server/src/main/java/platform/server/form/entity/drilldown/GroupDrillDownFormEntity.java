package platform.server.form.entity.drilldown;

import platform.base.BaseUtils;
import platform.base.SFunctionSet;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.interop.Compare;
import platform.server.classes.ValueClass;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.OrderEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;

public class GroupDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<GroupProperty.Interface<I>, GroupProperty<I>> {

    private PropertyDrawEntity implPropertyDraw;
    private GroupObjectEntity detailsGroup;

    public GroupDrillDownFormEntity(String sID, String caption, GroupProperty<I> property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }

    @Override
    protected void setupDrillDownForm() {
        ImMap<I, GroupProperty.Interface<I>> byInnerInterfaces = BaseUtils.immutableCast(
                property.getMapInterfaces().filterFnValues(new SFunctionSet<CalcPropertyInterfaceImplement<I>>() {
                    @Override
                    public boolean contains(CalcPropertyInterfaceImplement<I> element) {
                        return element instanceof PropertyInterface;
                    }
                }).toRevMap().reverse()
        );

        detailsGroup = new GroupObjectEntity(genID(), "");

        ImMap<I, ValueClass> innerClasses = property.getInnerInterfaceClasses();
        MMap<I, ObjectEntity> mInnerObjects = MapFact.mMap(MapFact.<I, ObjectEntity>override());
        for (int i = 0; i < innerClasses.size(); ++i) {
            ValueClass innerIntClass = innerClasses.getValue(i);
            I innerInterface = innerClasses.getKey(i);

            ObjectEntity innerObject;
            if (byInnerInterfaces.containsKey(innerInterface)) {
                innerObject = interfaceObjects.get(byInnerInterfaces.get(innerInterface));
            } else {
                innerObject = new ObjectEntity(genID(), innerIntClass, "");
                detailsGroup.add(innerObject);

                addPropertyDraw(BL.LM.objectValue, false, innerObject);
                addPropertyDraw(BL.LM.recognizeGroup, false, innerObject);
            }

            mInnerObjects.add(innerInterface, innerObject);
        }
        addGroupObject(detailsGroup);

        ImMap<I, ObjectEntity> innerObjects = mInnerObjects.immutable();

        //добавляем основные свойства
        ImList<CalcPropertyInterfaceImplement<I>> groupImplements = property.getProps();
        for (CalcPropertyInterfaceImplement<I> groupImplement : groupImplements) {
            if (groupImplement instanceof CalcPropertyMapImplement) {
                CalcPropertyMapImplement<PropertyInterface, I> mapImplement = (CalcPropertyMapImplement<PropertyInterface, I>) groupImplement;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(innerObjects).mapping;

                addFixedFilter(new NotNullFilterEntity(addPropertyObject(mapImplement.property, mapImplMapping)));
                if (mapImplement.property.isFull()) {
                    addPropertyDraw(mapImplement.property, mapImplMapping);
                }
            }
        }

        //добавляем BY свойства
        ImMap<GroupProperty.Interface<I>, CalcPropertyInterfaceImplement<I>> mapInterfaces = property.getMapInterfaces();
        for (int i = 0; i < mapInterfaces.size(); ++i) {
            GroupProperty.Interface<I> groupInterface = mapInterfaces.getKey(i);
            CalcPropertyInterfaceImplement<I> groupImplement = mapInterfaces.getValue(i);

            if (groupImplement instanceof CalcPropertyMapImplement) {
                CalcPropertyMapImplement<PropertyInterface, I> mapImplement = (CalcPropertyMapImplement<PropertyInterface, I>) groupImplement;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(innerObjects).mapping;

                addFixedFilter(new CompareFilterEntity(addPropertyObject(mapImplement.property, mapImplMapping), Compare.EQUALS, interfaceObjects.get(groupInterface)));
                //добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !BL.LM.recognizeGroup.hasChild(mapImplement.property)) {
                    if (mapImplement.property.isFull()) {
                        addPropertyDraw(mapImplement.property, mapImplMapping);
                    }
                }
            }
        }

        // добавляем порядки
        ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders = property.getOrders();
        for (int i = 0; i < orders.size(); ++i) {
            CalcPropertyInterfaceImplement<I> orderImplement = orders.getKey(i);
            Boolean asc = orders.getValue(i);

            OrderEntity orderEntity;
            if (orderImplement instanceof CalcPropertyMapImplement) {
                CalcPropertyMapImplement<PropertyInterface, I> mapImplement = (CalcPropertyMapImplement<PropertyInterface, I>) orderImplement;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(innerObjects).mapping;
                orderEntity = addPropertyObject(mapImplement.property, mapImplMapping);
            } else {
                I innerInterface = (I) orderImplement;
                orderEntity = innerObjects.get(innerInterface);
            }

            addFixedOrder(orderEntity, asc != null && asc);
        }

        implPropertyDraw = addPropertyDraw(property, interfaceObjects);
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        detailsContainer.add(design.getGroupObjectContainer(detailsGroup));

        valueContainer.add(design.get(implPropertyDraw));

        return design;
    }
}
