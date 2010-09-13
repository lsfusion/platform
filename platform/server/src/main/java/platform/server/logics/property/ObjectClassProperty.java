package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.SessionChanges;
import platform.server.session.DataSession;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.NullValue;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.CustomObjectInstance;
import platform.base.BaseUtils;
import platform.interop.action.ClientAction;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.sql.SQLException;

public class ObjectClassProperty extends UserProperty {

    private final BaseClass baseClass;

    public ObjectClassProperty(String SID, BaseClass baseClass) {
        super(SID, "Класс объекта", new ValueClass[]{baseClass});

        this.baseClass = baseClass;
    }

    protected ValueClass getValueClass() {
        return baseClass.objectClass;
    }

    public boolean isStored() {
        return false;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance remoteForm = executeForm.form;
        if(mapObjects.size()>0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance)
            remoteForm.changeClass((CustomObjectInstance) BaseUtils.singleValue(mapObjects), BaseUtils.singleValue(keys), (Integer)value.getValue());
        else
            remoteForm.session.changeClass(BaseUtils.singleValue(keys), baseClass.findConcreteClassID((Integer) value.getValue()));
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return modifier.getSession().getIsClassExpr(BaseUtils.singleValue(joinImplement),baseClass,changedWhere);
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return modifier.newChanges().addChanges(new SessionChanges(modifier.getSession(), true));
    }
}
