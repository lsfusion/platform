package platform.server.logics.property.actions;

import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ObjectClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;
import java.util.Map;

// именно наследованием, чтобы getSimpleAdd, дизайны и т.п. подтянулись
public class FormAddObjectActionProperty extends AddObjectActionProperty<PropertyInterface, PropertyInterface> {
    
    private final ObjectEntity objectEntity;

    public FormAddObjectActionProperty(String sID, CustomClass customClass, boolean forceDialog, ObjectEntity objectEntity) {
        super(sID, customClass!=null ? customClass : (CustomClass) objectEntity.baseClass, forceDialog, null);

        this.objectEntity = objectEntity;
    }

   @Override
    protected void executeRead(ExecutionContext<PropertyInterface> context, Map<PropertyInterface, KeyExpr> innerKeys, Map<PropertyInterface, Expr> innerExprs, ConcreteCustomClass readClass) throws SQLException {
        assert where==null;

        context.addFormObject(objectEntity, readClass);
    }
}
