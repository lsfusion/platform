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
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// AutoFormEntity that is initialized not during parsing, but after the logics were completely initialized
public class AutoFinalFormEntity extends AutoFormEntity {

    protected Version baseVersion;
    protected BaseLogicsModule LM;

    public AutoFinalFormEntity(LocalizedString caption, BaseLogicsModule LM) {
        super(true, LM.getVersion());

        this.LM = LM;
        baseVersion = LM.getVersion();

        getInitDesign().mainContainer.setCaption(caption, baseVersion);
    }

    protected DefaultFormView getInitDesign() {
        return (DefaultFormView) view;
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
        for (int i = 0, size = objectsSet.size(); i < size; i++) {
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
        ObjectEntity object = new ObjectEntity(genID, baseClass);

        addGroupObjectEntity(SetFact.singletonOrder(object));

        return object;
    }

    protected GroupObjectEntity addGroupObjectEntity(ImOrderSet<ObjectEntity> objects) {
        return addGroupObjectEntity(LM, objects, baseVersion);
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LAP<P, ?> property, ImOrderSet<ObjectEntity> objects) {
        return addPropertyDraw(property, baseVersion, objects);
    }

    public void movePropertyDraw(PropertyDrawEntity property, ComplexLocation<PropertyDrawEntity> location) {
        movePropertyDraw(property, location, baseVersion);
    }

    public PropertyDrawEntity<?, ?> addValuePropertyDraw(BaseLogicsModule LM, ObjectEntity object) {
        return addValuePropertyDraw(LM, object, baseVersion);
    }

    public void setEditType(PropertyDrawEntity<?, ?> property, PropertyEditType editType) {
        property.setEditType(editType, baseVersion);
    }

    public void setDefaultChangeEventScope(PropertyDrawEntity<?, ?> property, FormSessionScope scope) {
        property.setDefaultChangeEventScope(scope, baseVersion);
    }

    public <I extends PropertyInterface, P extends ActionOrProperty<I>> PropertyDrawEntity<I, ?> addPropertyDraw(P property, ImRevMap<I, ObjectEntity> mapping) {
        return addPropertyDraw(property, mapping, baseVersion);
    }

    protected void finalizeInit() {
        finalizeInit(baseVersion);
    }

    protected void setEditType(PropertyEditType editType) {
        setEditType(editType, baseVersion);
    }

    public void setEditType(PropertyEditType editType, Version version) {
        for (PropertyDrawEntity propertyView : getNFPropertyDrawsIt(version)) {
            if (!propertyView.isSystem)
                setEditType(propertyView, editType, version);
        }
    }

    public void addFixedFilter(FilterEntity filter) {
        addFixedFilter(filter, baseVersion);
    }

    public void addFixedOrder(OrderEntity order, boolean descending) {
        addFixedOrder(order, descending, baseVersion);
    }

    public void removeComponent(PropertyDrawEntity<?, ?> property) {
        DefaultFormView design = getInitDesign();
        design.removeComponent(design.get(property), baseVersion);
    }

}
