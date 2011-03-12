package platform.server.logics.property;

import platform.server.classes.DataClass;
import platform.server.classes.StaticClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.InfiniteExpr;

public class InfiniteClassProperty extends ClassProperty<DataClass> {

    public InfiniteClassProperty(String sID, String caption, ValueClass[] classes, DataClass dataClass) {
        super(sID, caption, classes, dataClass);
    }

    protected Expr getStaticExpr() {
        return new InfiniteExpr(staticClass);
    }
}
