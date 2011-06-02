package platform.server.logics.property;

import platform.interop.action.ClientAction;
import platform.server.classes.ActionClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public abstract class ActionProperty extends ExecuteProperty {

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    // FormInstance в качестве параметра нужен поскольку действие, как правило, приводит к какой-то реакции со стороны формы
    // RemoteForm нужен потому, что нужно создавать новый объект RMI, а для этого нужен как минимум порт - правда это в дальнейшем нужно переделать
    public abstract void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException;

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return ValueClassProperty.getIsClassUsedChanges(interfaces, modifier);
    }
    
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return getValueClass().getDefaultExpr().and(ClassProperty.getIsClassWhere(joinImplement, modifier, changedWhere));
    }

    public DataClass getValueClass() {
        return ActionClass.instance;
    }
}
