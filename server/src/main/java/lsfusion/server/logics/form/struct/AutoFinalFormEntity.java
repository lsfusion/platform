package lsfusion.server.logics.form.struct;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterRevValueMap;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// AutoFormEntity that is initialized not during parsing, but after the logics were completely initialized
public class AutoFinalFormEntity extends AutoFormEntity {

    protected Version baseVersion;

    public AutoFinalFormEntity(LocalizedString caption, BaseLogicsModule LM) {
        super(caption, LM.getVersion());

        baseVersion = LM.getVersion();
    }

    protected ImList<ActionOrPropertyClassImplement> getActionOrProperties(Group group, ValueClass cls) {
        ImSet<ValueClassWrapper> classes = cls != null ? SetFact.singleton(new ValueClassWrapper(cls)) : SetFact.EMPTY();

        return getActionOrProperties(group, classes);
    }

    private ImList<ActionOrPropertyClassImplement> getActionOrProperties(Group group, ImSet<ValueClassWrapper> classes) {
        return group.getActionOrProperties(classes, classes.group(key -> key.valueClass), isNoAny());
    }

    protected boolean isNoAny() {
        return false;
    }

    public void addPropertyDraw(ObjectEntity object, Group group) {
        addPropertyDraw(group, false, SetFact.singletonOrder(object));
    }

    protected void addPropertyDraw(Group group, boolean prev, ImOrderSet<ObjectEntity> objects) {
        ImSet<ObjectEntity> objectsSet = objects.getSet();
        ImFilterRevValueMap<ObjectEntity, ValueClassWrapper> mObjectToClass = objectsSet.mapFilterRevValues();
        for(int i=0,size=objectsSet.size();i<size;i++) {
            ObjectEntity object = objectsSet.get(i);
            if (object.baseClass != null)
                mObjectToClass.mapValue(i, new ValueClassWrapper(object.baseClass));
        }
        ImRevMap<ObjectEntity, ValueClassWrapper> objectToClass = mObjectToClass.immutableRevValue();
        ImSet<ValueClassWrapper> valueClasses = objectToClass.valuesSet();

        // here can be more precise heuristics than implemented in FormDataManager.getPrintTable (calculating expr and putting expr itself (not its values)  in a set)

        ImOrderSet<ValueClassWrapper> orderInterfaces = objects.mapOrder(objectToClass);
        for (ActionOrPropertyClassImplement implement : getActionOrProperties(group, valueClasses)) {
            ImSet<ValueClassWrapper> wrappers = implement.mapping.valuesSet();
            ImOrderSet<ObjectEntity> filterObjects = objects.filterOrderIncl(objectToClass.filterValuesRev(wrappers).keys());
            addPropertyDraw(implement.createLP(orderInterfaces.filterOrderIncl(wrappers), prev), filterObjects);
        }
    }

    public ObjectEntity addSingleGroupObject(ValueClass baseClass) {
        GroupObjectEntity groupObject = new GroupObjectEntity(genID(), (TreeGroupEntity) null);
        ObjectEntity object = new ObjectEntity(genID(), baseClass, baseClass != null ? baseClass.getCaption() : LocalizedString.NONAME, baseClass == null);
        groupObject.add(object);
        addGroupObject(groupObject);

        return object;
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LAP<P, ?> property, ImOrderSet<ObjectEntity> objects) {
        return addPropertyDraw(property, baseVersion, objects);
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LAP<P, ?> property, ComplexLocation<PropertyDrawEntity> location, ImOrderSet<ObjectEntity> objects) {
        return addPropertyDraw(property, location, baseVersion, objects);
    }

    public PropertyDrawEntity<?> addValuePropertyDraw(BaseLogicsModule LM, ObjectEntity object) {
        return addValuePropertyDraw(LM, object, baseVersion);
    }

    public <I extends PropertyInterface, P extends ActionOrProperty<I>> PropertyDrawEntity<I> addPropertyDraw(P property, ImRevMap<I, ObjectEntity> mapping) {
        return addPropertyDraw(property, mapping, baseVersion);
    }

    protected void addGroupObject(GroupObjectEntity groupObject) {
        addGroupObject(groupObject, baseVersion);
    }

    protected void finalizeInit() {
        finalizeInit(baseVersion);
    }

    protected void setNFEditType(PropertyEditType editType) {
        setNFEditType(editType, baseVersion);
    }

    public void addFixedFilter(FilterEntity filter) {
        addFixedFilter(filter, baseVersion);
    }

    public void addFixedOrder(OrderEntity order, boolean descending) {
        addFixedOrder(order, descending, baseVersion);
    }
}
