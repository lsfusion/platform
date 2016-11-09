package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

// именно наследованием, чтобы getSimpleAdd, дизайны и т.п. подтянулись
public class FormAddObjectActionProperty extends AddObjectActionProperty<PropertyInterface, PropertyInterface> {
    
    private final ObjectEntity objectEntity;

    public FormAddObjectActionProperty(CustomClass customClass, ObjectEntity objectEntity) {
        super(customClass, false, null);

        this.objectEntity = objectEntity;
    }

   @Override
    protected void executeRead(ExecutionContext<PropertyInterface> context, ImRevMap<PropertyInterface, KeyExpr> innerKeys, ImMap<PropertyInterface, Expr> innerExprs, ConcreteCustomClass readClass) throws SQLException, SQLHandledException {
        assert where==null;

        context.addFormObject(objectEntity, readClass);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<PropertyInterface> entity, FormEntity<?> form, Version version) {
        super.proceedDefaultDraw(entity, form, version);
        
        entity.toDraw = objectEntity.groupTo; 
    }
}
