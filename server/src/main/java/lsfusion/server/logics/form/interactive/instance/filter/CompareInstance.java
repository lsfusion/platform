package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.changed.Updated;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.Property;

import java.sql.SQLException;

public interface CompareInstance extends Updated {

//    AndClassSet getValueClass(GroupObjectInstance ClassGroup) {return null;}

    Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier) throws SQLException, SQLHandledException;

    Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException;
    
    Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException;

    ImCol<ObjectInstance> getObjectInstances();
}
