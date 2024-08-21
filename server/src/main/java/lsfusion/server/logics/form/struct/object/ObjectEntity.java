package lsfusion.server.logics.form.struct.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.interactive.action.input.InputOrderEntity;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import static lsfusion.server.physics.dev.i18n.LocalizedString.create;

public class ObjectEntity extends IdentityObject implements OrderEntity<PropertyObjectInterfaceInstance>, ObjectSelector {

    public GroupObjectEntity groupTo;

    public LocalizedString caption;

    public LocalizedString getCaption() {
        return (caption != null && !BaseUtils.isRedundantString(caption.getSourceString()))
               ? caption
               : baseClass != null && !BaseUtils.isRedundantString(baseClass.toString())
                 ? create(baseClass.toString())
                 : create("{logics.undefined.object}");
    }

    public ValueClass baseClass;
    private boolean noClasses;

    public String integrationSID;

    public boolean isValue;

    private boolean finalizedValueProperty;
    private Property<?> valueProperty;
    @NFLazy
    public Property<?> getNFValueProperty() {
        if(finalizedValueProperty)
            return valueProperty;

        Property<?> prop = valueProperty;
        if(prop==null) {
            prop = PropertyFact.createDataPropRev("VALUE", this, baseClass);
            valueProperty = prop;
        }
        return prop;
    }

    public Property<?> getValueProperty() {
        finalizedValueProperty = true;
        return valueProperty;
    }

    public ObjectEntity() {

    }
    
    public ObjectEntity(int ID, ValueClass baseClass, LocalizedString caption) {
        this(ID, null, baseClass, caption);
    }
    public ObjectEntity(int ID, ValueClass baseClass, LocalizedString caption, boolean noClasses) {
        this(ID, null, baseClass, caption, noClasses);
    }
    public ObjectEntity(int ID, String sID, ValueClass baseClass, LocalizedString caption) {
        this(ID, sID, baseClass, caption, false);
    }
    public ObjectEntity(int ID, String sID, ValueClass baseClass, LocalizedString caption, boolean noClasses) {
        super(ID);
        this.sID = sID != null ? sID : "obj" + ID;
        this.caption = caption;
        this.baseClass = baseClass;
        this.noClasses = noClasses;
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
        assert groupTo.isPanel();
        // we want to have null value if targetProp is null
        return lm.addJoinAProp(lm.addOSAProp(this, null), targetProp).getImplement();
    }

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID != null ? integrationSID : getSID();
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
}
