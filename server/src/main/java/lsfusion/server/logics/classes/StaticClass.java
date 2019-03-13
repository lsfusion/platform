package lsfusion.server.logics.classes;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.Expr;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);

    ResolveClassSet getResolveSet();

    boolean isZero(Object object);
}
