package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.action.ClientAction;
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
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.SimpleChanges;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ObjectClassProperty extends ExecuteProperty {

    private final BaseClass baseClass;

    public ObjectClassProperty(String SID, BaseClass baseClass) {
        super(SID, "Класс объекта", new ValueClass[]{baseClass});

        this.baseClass = baseClass;
    }

    protected ValueClass getValueClass() {
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

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        FormInstance remoteForm = executeForm.form;
        if(mapObjects.size()>0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance)
            remoteForm.changeClass((CustomObjectInstance) BaseUtils.singleValue(mapObjects), BaseUtils.singleValue(keys), (Integer)value.getValue());
        else
            session.changeClass(BaseUtils.singleValue(keys), baseClass.findConcreteClassID((Integer) value.getValue()));
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return modifier.getSession().getIsClassExpr(BaseUtils.singleValue(joinImplement),baseClass,changedWhere);
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return modifier.newChanges().addChanges(new SimpleChanges(modifier.getChanges(), true));
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
        super.proceedDefaultDraw(entity, form);
        PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(entity.propertyObject.mapping);
        if(mapObject instanceof ObjectEntity && !((CustomClass)((ObjectEntity)mapObject).baseClass).hasChildren())
            entity.forceViewType = ClassViewType.HIDE;
    }
}
