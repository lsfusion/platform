package platform.server.logics.property.actions;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

// именно наследованием, чтобы getSimpleAdd, дизайны и т.п. подтянулись
public class FormAddObjectActionProperty extends AddObjectActionProperty<PropertyInterface, PropertyInterface> {
    
    private final ObjectEntity objectEntity;

    public FormAddObjectActionProperty(String sID, CustomClass customClass, ObjectEntity objectEntity) {
        super(sID, customClass, false, null);

        this.objectEntity = objectEntity;
    }

   @Override
    protected void executeRead(ExecutionContext<PropertyInterface> context, ImRevMap<PropertyInterface, KeyExpr> innerKeys, ImMap<PropertyInterface, Expr> innerExprs, ConcreteCustomClass readClass) throws SQLException {
        assert where==null;

        context.addFormObject(objectEntity, readClass);
    }
}
