package lsfusion.server.logics.form.struct.object;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.action.input.InputOrderEntity;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.design.object.ObjectView;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.IdentityEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import static lsfusion.server.physics.dev.i18n.LocalizedString.create;

public class ObjectEntity extends IdentityEntity<ObjectEntity, GroupObjectEntity> implements OrderEntity<PropertyObjectInterfaceInstance, ObjectEntity>, ObjectSelector {

    public GroupObjectEntity groupTo;

    private final NFProperty<LocalizedString> caption = NFFact.property();

    public void setCaption(LocalizedString caption, Version version) {
        this.caption.set(caption, version);
    }

    public LocalizedString getCaption() {
        LocalizedString explicit = caption.get();
        if (explicit != null)
            return explicit;

        if (baseClass != null)
            return baseClass.getCaption();

        return create("{logics.undefined.object}");
    }

    public LocalizedString getNFCaption(Version version) {
        LocalizedString explicit = caption.getNF(version);
        if (explicit != null)
            return explicit;

        if (baseClass != null)
            return baseClass.getCaption();

        return create("{logics.undefined.object}");
    }

    public ValueClass baseClass;
    private boolean noClasses;

    private final NFProperty<String> integrationSID = NFFact.property();

    private final FormEntity.ExProperty valueProperty;
    @NFLazy
    public Property<?> getNFValueProperty(Version version) {
        return valueProperty.getNF(version);
    }

    public Property<?> getValueProperty() {
        return valueProperty.get();
    }

    public ObjectEntity(IDGenerator ID, ValueClass baseClass) {
        this(ID, baseClass, baseClass == null);
    }
    public ObjectEntity(IDGenerator ID, ValueClass baseClass, boolean noClasses) {
        this(ID, null, baseClass, noClasses);
    }

    @Override
    protected String getDefaultSIDPrefix() {
        return "obj";
    }

    public ObjectEntity(IDGenerator ID, String sID, ValueClass baseClass, boolean noClasses) {
        super(ID, sID, null);

        this.baseClass = baseClass;
        this.noClasses = noClasses;

        valueProperty = new FormEntity.ExProperty(() -> PropertyFact.createDataPropRev("VALUE", this, baseClass));
    }

    public ObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption());
    }

    public AndClassSet getAndClassSet() {
        return baseClass.getUpSet();
    }

    public ResolveClassSet getResolveClassSet() {
        return baseClass.getResolveSet();
    }

    @Override
    public Type getType() {
        if(noClasses())
            return null;
        return baseClass.getType();
    }
    
    @Override
    public Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) {
        return mapExprs.get(this);
    }

    public ObjectValue getObjectValue(ImMap<ObjectEntity, ? extends ObjectValue> mapObjects) {
        ObjectValue objectValue = mapObjects.get(this);
        if(objectValue != null)
            return objectValue;
        return NullValue.instance;
    }

    @Override
    public GroupObjectEntity getApplyObject(FormEntity formEntity, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return groupTo;
    }

    public <T extends PropertyInterface> ActionMapImplement<?, T> getSeekPanelAction(BaseLogicsModule lm, LP targetProp) {
//        assert groupTo.isPanel();
        // we want to have null value if targetProp is null
        return lm.addJoinAProp(lm.addOSAProp(this, null), targetProp).getImplement();
    }

    public void setIntegrationSID(String integrationSID, Version version) {
        this.integrationSID.set(integrationSID, version);
    }

    public String getIntegrationSID() {
        String explicit = integrationSID.get();
        return explicit != null ? explicit : getSID();
    }

    @Override
    public <T extends PropertyInterface> PropertyInterfaceImplement<T> getImplement(ImRevMap<ObjectEntity, T> mapObjects) {
        return mapObjects.get(this);
    }

    @Override
    public boolean noClasses() {
        return noClasses;
    }

    @IdentityInstanceLazy
    public StaticParamNullableExpr getParamExpr() {
        return new StaticParamNullableExpr(baseClass);
    }

    @Override
    public ImSet<ObjectEntity> getObjects() {
        return SetFact.singleton(this);
    }

    @Override
    public <T extends PropertyInterface> InputOrderEntity<?, T> getInputOrderEntity(ObjectEntity object, ImRevMap<ObjectEntity, T> mapObjects) {
        return null; // temporary
    }

    public ObjectView view;

    // copy-constructor
    protected ObjectEntity(ObjectEntity src, ObjectMapping mapping) {
        super(src, mapping);

        baseClass = src.baseClass;
        noClasses = src.noClasses;

        valueProperty = mapping.get(src.valueProperty);
        groupTo = mapping.get(src.groupTo);
        view = mapping.get(src.view);
    }

    @Override
    public void extend(ObjectEntity src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(caption, src.caption);
        mapping.sets(integrationSID, src.integrationSID);
    }

//    @Override
//    public GroupObjectEntity getAddParent(ObjectMapping mapping) {
//        return groupTo;
//    }
//    @Override
//    public ObjectEntity getAddChild(GroupObjectEntity groupObjectEntity, ObjectMapping mapping) {
//        ObjectEntity explicitChild = (ObjectEntity) mapping.addObjects.get(this);
//        if(explicitChild != null)
//            return explicitChild;
//
//        return groupObjectEntity.getObject(getSID());
//    }
    @Override
    public ObjectEntity copy(ObjectMapping mapping) {
        return new ObjectEntity(this, mapping);
    }
}
