package platform.server.classes;

import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.data.expr.Expr;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);

    String getString(Object value, SQLSyntax syntax);
}
