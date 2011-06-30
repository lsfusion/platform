package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.List;
import java.util.Map;

public abstract class ComplexIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public ComplexIncrementProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return false;
    }
}
