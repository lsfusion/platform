package lsfusion.server.classes;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSyntax;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);

    String getString(Object value, SQLSyntax syntax);
}
