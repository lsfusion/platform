package platform.server.classes;

import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);

    String getString(Object value, SQLSyntax syntax);
}
