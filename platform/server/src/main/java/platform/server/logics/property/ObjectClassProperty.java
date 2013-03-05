package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.interop.ClassViewType;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.IdentityLazy;
import platform.server.classes.BaseClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.actions.ChangeClassActionProperty;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

public class ObjectClassProperty extends AggregateProperty<ClassPropertyInterface> {

    private final BaseClass baseClass;

    public ObjectClassProperty(String SID, BaseClass baseClass) {
        super(SID, ServerResourceBundle.getString("classes.object.class"), IsClassProperty.getInterfaces(new ValueClass[]{baseClass}));

        this.baseClass = baseClass;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return joinImplement.singleValue().classExpr(baseClass);
    }
    
    public Expr getExpr(Expr expr, Modifier modifier) {
        return getExpr(MapFact.singleton(getInterface(), expr), modifier);
    }

    @Override
    protected boolean useSimpleIncrement() {
        return true;
    }

    private ClassPropertyInterface getInterface() {
        return interfaces.single();
    }

    @Override
    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, ClassPropertyInterface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return ChangeClassActionProperty.create(null, false, baseClass).getImplement(SetFact.singletonOrder(getInterface()));
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        PropertyObjectInterfaceEntity mapObject = entity.propertyObject.mapping.singleValue();
        if(mapObject instanceof ObjectEntity && !((CustomClass)((ObjectEntity)mapObject).baseClass).hasChildren())
            entity.forceViewType = ClassViewType.HIDE;
    }
}
