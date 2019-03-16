package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.change.AddObjectAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

// именно наследованием, чтобы getSimpleAdd, дизайны и т.п. подтянулись
public class FormAddObjectActionProperty extends AddObjectAction<PropertyInterface, PropertyInterface> {
    
    private final ObjectEntity objectEntity;

    public FormAddObjectActionProperty(CustomClass customClass, final ObjectEntity objectEntity) {
        super(customClass, null, true);

        this.objectEntity = objectEntity;
    }

   @Override
    protected void executeRead(ExecutionContext<PropertyInterface> context, ImRevMap<PropertyInterface, KeyExpr> innerKeys, ImMap<PropertyInterface, Expr> innerExprs, ConcreteCustomClass readClass) throws SQLException, SQLHandledException {
        assert where==null;

        context.formAddObject(objectEntity, readClass);
    }
}
