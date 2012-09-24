package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.ClassViewType;
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

import java.util.Collections;
import java.util.Map;

public class ObjectClassProperty extends AggregateProperty<ClassPropertyInterface> {

    private final BaseClass baseClass;

    public ObjectClassProperty(String SID, BaseClass baseClass) {
        super(SID, ServerResourceBundle.getString("classes.object.class"), IsClassProperty.getInterfaces(new ValueClass[]{baseClass}));

        this.baseClass = baseClass;

        finalizeInit();
    }

    protected QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return QuickSet.EMPTY();
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return BaseUtils.singleValue(joinImplement).classExpr(baseClass);
    }
    
    public Expr getExpr(Expr expr, Modifier modifier) {
        return getExpr(Collections.singletonMap(getInterface(), expr), modifier);
    }

    @Override
    protected boolean useSimpleIncrement() {
        return true;
    }

    private ClassPropertyInterface getInterface() {
        return BaseUtils.single(interfaces);
    }

    @Override
    @IdentityLazy
    public ActionPropertyMapImplement<?, ClassPropertyInterface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return ChangeClassActionProperty.create(null, false, baseClass).getImplement(Collections.singletonList(getInterface()));
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(entity.propertyObject.mapping);
        if(mapObject instanceof ObjectEntity && !((CustomClass)((ObjectEntity)mapObject).baseClass).hasChildren())
            entity.forceViewType = ClassViewType.HIDE;
    }
}
