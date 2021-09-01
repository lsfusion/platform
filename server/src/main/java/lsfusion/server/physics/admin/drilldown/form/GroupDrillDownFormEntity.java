package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.set.GroupProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class GroupDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<GroupProperty.Interface<I>, GroupProperty<I>> {

    private PropertyDrawEntity implPropertyDraw;
    private GroupObjectEntity detailsGroup;

    public GroupDrillDownFormEntity(LocalizedString caption, GroupProperty<I> property, LogicsModule LM) {
        super(caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();

        ImMap<I, GroupProperty.Interface<I>> byInnerInterfaces = BaseUtils.immutableCast(
                property.getMapInterfaces().toRevMap(property.getReflectionOrderInterfaces()).filterFnValuesRev(element -> element instanceof PropertyInterface).reverse()
        );

        detailsGroup = new GroupObjectEntity(genID(), "");

        ImMap<I, ValueClass> innerClasses = property.getInnerInterfaceClasses();
        MRevMap<I, ObjectEntity> mInnerObjects = MapFact.mRevMap();
        MAddSet<ObjectEntity> usedObjects = SetFact.mAddSet();

        for (int i = 0; i < innerClasses.size(); ++i) {
            ValueClass innerIntClass = innerClasses.getValue(i);
            I innerInterface = innerClasses.getKey(i);

            ObjectEntity innerObject = null;
            GroupProperty.Interface<I> byInterface = byInnerInterfaces.get(innerInterface);
            if (byInterface != null) {
                innerObject = interfaceObjects.get(byInterface);
            } 
            if(innerObject == null || usedObjects.add(innerObject)) {
                innerObject = new ObjectEntity(genID(), innerIntClass, LocalizedString.NONAME, innerIntClass == null);
                detailsGroup.add(innerObject);

                addValuePropertyDraw(LM, innerObject, version);
                addPropertyDraw(innerObject, version, LM.getRecognizeGroup());
            }

            mInnerObjects.revAdd(innerInterface, innerObject);
        }
        addGroupObject(detailsGroup, version);

        ImRevMap<I, ObjectEntity> innerObjects = mInnerObjects.immutableRev();
        
        //добавляем основные свойства
        ImList<PropertyInterfaceImplement<I>> groupImplements = property.getProps();
        for (PropertyInterfaceImplement<I> groupImplement : groupImplements) {
            if (groupImplement instanceof PropertyMapImplement) {
                PropertyMapImplement<PropertyInterface, I> mapImplement = (PropertyMapImplement<PropertyInterface, I>) groupImplement;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(innerObjects).mapping;

                addFixedFilter(new FilterEntity(addPropertyObject(mapImplement.property, mapImplMapping)), version);
                if (mapImplement.property.isDrillFull()) {
                    addPropertyDraw(mapImplement.property, mapImplMapping, version);
                }
            }
        }

        //добавляем BY свойства
        ImMap<GroupProperty.Interface<I>, PropertyInterfaceImplement<I>> mapInterfaces = property.getMapInterfaces();
        for (int i = 0; i < mapInterfaces.size(); ++i) {
            GroupProperty.Interface<I> groupInterface = mapInterfaces.getKey(i);
            PropertyInterfaceImplement<I> groupImplement = mapInterfaces.getValue(i);

            if (groupImplement instanceof PropertyMapImplement || !innerObjects.containsKey((I) groupImplement)) {
                PropertyRevImplement filterProp = PropertyFact.createCompare(groupImplement, (PropertyInterface) groupInterface, Compare.EQUALS).mapRevImplement(MapFact.addRevExcl(innerObjects, groupInterface, interfaceObjects.get(groupInterface)));
                addFixedFilter(new FilterEntity(addPropertyObject(filterProp)), version);

                if(groupImplement instanceof PropertyMapImplement) {
                    PropertyMapImplement<PropertyInterface, I> mapImplement = (PropertyMapImplement<PropertyInterface, I>) groupImplement;
                    ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(innerObjects).mapping;
                    if (mapImplMapping.size() != 1 || !LM.getRecognizeGroup().hasChild(mapImplement.property)) {
                        if (mapImplement.property.isDrillFull()) {
                            addPropertyDraw(mapImplement.property, mapImplMapping, version);
                        }
                    }
                }
            }
        }

        // добавляем порядки
        ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders = property.getOrders();
        for (int i = 0; i < orders.size(); ++i) {
            PropertyInterfaceImplement<I> orderImplement = orders.getKey(i);
            Boolean asc = orders.getValue(i);

            OrderEntity orderEntity;
            if (orderImplement instanceof PropertyMapImplement) {
                PropertyMapImplement<PropertyInterface, I> mapImplement = (PropertyMapImplement<PropertyInterface, I>) orderImplement;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(innerObjects).mapping;
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

        detailsContainer.add(design.getBoxContainer(detailsGroup), version);

        valueContainer.add(design.get(implPropertyDraw), version);

        return design;
    }
}
