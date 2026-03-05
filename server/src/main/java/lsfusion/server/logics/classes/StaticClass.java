package lsfusion.server.logics.classes;

import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.value.ValueProperty;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);

    ResolveClassSet getResolveSet();

    ValueProperty getProperty(Object value);
}
