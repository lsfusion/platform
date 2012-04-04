package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.ClassViewType;
import platform.server.classes.BaseClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.ServerResourceBundle;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjectClassProperty extends ExecuteProperty {

    private final BaseClass baseClass;

    public ObjectClassProperty(String SID, BaseClass baseClass) {
        super(SID, ServerResourceBundle.getString("classes.object.class"), new ValueClass[]{baseClass});

        this.baseClass = baseClass;

        finalizeInit();
    }

    public ValueClass getValueClass() {
        return baseClass.objectClass;
    }

    @Override
    public Type getEditorType(Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        if(mapObjects.size()>0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance) {
            CustomObjectInstance object = (CustomObjectInstance) BaseUtils.singleValue(mapObjects);
            return object.baseClass.getActionClass(object.currentClass);
        } else
            return super.getEditorType(mapObjects);
    }

    public void execute(ExecutionContext context) throws SQLException {
        if (context.isInFormSession() && context.getSingleObjectInstance() instanceof ObjectInstance) {
            context.getFormInstance().changeClass((CustomObjectInstance) context.getSingleObjectInstance(), context.getSingleKeyValue(), (Integer) context.getValueObject());
        } else {
            context.getSession().changeClass(context.getSingleKeyValue(), baseClass.findConcreteClassID((Integer) context.getValueObject()), context.isGroupLast());
        }
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return QuickSet.EMPTY();
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return BaseUtils.singleValue(joinImplement).classExpr(baseClass);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(entity.propertyObject.mapping);
        if(mapObject instanceof ObjectEntity && !((CustomClass)((ObjectEntity)mapObject).baseClass).hasChildren())
            entity.forceViewType = ClassViewType.HIDE;
    }

    @Override
    public Set<Property> getChangeProps() {
        return baseClass.getChildProps();
    }

    public Set<Property> getUsedProps() {
        return new HashSet<Property>();
    }
}
