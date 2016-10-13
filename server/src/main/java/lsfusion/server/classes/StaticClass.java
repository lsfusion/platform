package lsfusion.server.classes;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSyntax;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);

    ResolveClassSet getResolveSet();

    boolean isZero(Object object);
}
