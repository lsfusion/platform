package lsfusion.server.logics.classes;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.classes.sets.ResolveClassSet;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);

    ResolveClassSet getResolveSet();

    boolean isZero(Object object);
}
