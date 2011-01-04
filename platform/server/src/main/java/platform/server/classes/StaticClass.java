package platform.server.classes;

import platform.server.logics.DataObject;
import platform.server.data.expr.Expr;

public interface StaticClass extends ConcreteValueClass {

    Expr getStaticExpr(Object value);
}
