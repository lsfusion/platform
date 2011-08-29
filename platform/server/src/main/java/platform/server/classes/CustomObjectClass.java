package platform.server.classes;

import platform.server.data.expr.query.OrderExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.Query;
import platform.server.logics.ServerResourceBundle;

public class CustomObjectClass extends ConcreteCustomClass {

    public CustomObjectClass(String sID, String caption, CustomClass... parents) {
        super(sID, caption, parents);
    }

    public Integer stat = 100000;
    public int getCount() {
        return stat;
    }
}
