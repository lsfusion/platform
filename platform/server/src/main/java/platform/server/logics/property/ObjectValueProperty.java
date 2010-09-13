package platform.server.logics.property;

import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.SessionChanges;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.interop.action.ClientAction;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;

public class ObjectValueProperty extends UserProperty {

    private final ValueClass valueClass;

    public ObjectValueProperty(String SID, ValueClass valueClass) {
        super(SID, "Класс объекта", new ValueClass[]{valueClass});

        this.valueClass = valueClass;
    }

    protected ValueClass getValueClass() {
        return valueClass;
    }

    public boolean isStored() {
        return false;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> remoteForm = executeForm.form;
        if(mapObjects.size()>0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance)
            actions.addAll(remoteForm.changeObject((ObjectInstance)BaseUtils.singleValue(mapObjects), value.getValue(), executeForm));
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return BaseUtils.singleValue(joinImplement).and(ClassProperty.getIsClassWhere(joinImplement, modifier, changedWhere));
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return ClassProperty.getIsClassUsedChanges(interfaces, modifier);
    }
}
