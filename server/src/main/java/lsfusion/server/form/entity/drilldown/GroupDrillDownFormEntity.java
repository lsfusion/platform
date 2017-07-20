package lsfusion.server.form.entity.drilldown;

import lsfusion.base.BaseUtils;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.OrderEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.CompareFilterEntity;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.GroupProperty;
import lsfusion.server.logics.property.PropertyInterface;

public class GroupDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<GroupProperty.Interface<I>, GroupProperty<I>> {

    private PropertyDrawEntity implPropertyDraw;
    private GroupObjectEntity detailsGroup;

    public GroupDrillDownFormEntity(String canonicalName, LocalizedString caption, GroupProperty<I> property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();

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
                innerObject = new ObjectEntity(genID(), innerIntClass, LocalizedString.create(""));
                detailsGroup.add(innerObject);

                addPropertyDraw(LM.baseLM.getObjValueProp(this, innerObject), version, innerObject);
                addPropertyDraw(LM.recognizeGroup, true, version, innerObject);
            }

            mInnerObjects.add(innerInterface, innerObject);
        }
        addGroupObject(detailsGroup, version);

        ImMap<I, ObjectEntity> innerObjects = mInnerObjects.immutable();

        //добавляем основные свойства
        ImList<CalcPropertyInterfaceImplement<I>> groupImplements = property.getProps();
        for (CalcPropertyInterfaceImplement<I> groupImplement : groupImplements) {
            if (groupImplement instanceof CalcPropertyMapImplement) {
                CalcPropertyMapImplement<PropertyInterface, I> mapImplement = (CalcPropertyMapImplement<PropertyInterface, I>) groupImplement;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(innerObjects).mapping;

                addFixedFilter(new NotNullFilterEntity(addPropertyObject(mapImplement.property, mapImplMapping)), version);
                if (mapImplement.property.isDrillFull()) {
                    addPropertyDraw(mapImplement.property, mapImplMapping, version);
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

                addFixedFilter(new CompareFilterEntity(addPropertyObject(mapImplement.property, mapImplMapping), Compare.EQUALS, interfaceObjects.get(groupInterface)), version);
                //добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !LM.recognizeGroup.hasChild(mapImplement.property)) {
                    if (mapImplement.property.isDrillFull()) {
                        addPropertyDraw(mapImplement.property, mapImplMapping, version);
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

            addFixedOrder(orderEntity, asc != null && asc, version);
        }

        implPropertyDraw = addPropertyDraw(property, interfaceObjects, version);
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        detailsContainer.add(design.getGroupObjectContainer(detailsGroup), version);

        valueContainer.add(design.get(implPropertyDraw), version);

        return design;
    }
}
