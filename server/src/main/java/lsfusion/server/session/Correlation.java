package lsfusion.server.session;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;

import java.sql.SQLException;

public interface Correlation<K> {
    
    Expr getExpr(ImMap<K, ? extends Expr> mapExprs);
    Expr getExpr(ImMap<K, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException;
    
    Type getType();
    
    CustomClass getCustomClass();
}
