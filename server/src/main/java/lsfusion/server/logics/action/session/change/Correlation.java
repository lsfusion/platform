package lsfusion.server.logics.action.session.change;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.Property;

import java.sql.SQLException;

public interface Correlation<K> {
    
    Expr getExpr(ImMap<K, ? extends Expr> mapExprs);
    Expr getExpr(ImMap<K, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException;
    
    Type getType();
    
    Property<?> getProperty();
    
    CustomClass getCustomClass();
}
