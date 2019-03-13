package lsfusion.server.session;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.CalcProperty;

import java.sql.SQLException;

public interface Correlation<K> {
    
    Expr getExpr(ImMap<K, ? extends Expr> mapExprs);
    Expr getExpr(ImMap<K, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException;
    
    Type getType();
    
    CalcProperty<?> getProperty();
    
    CustomClass getCustomClass();
}
