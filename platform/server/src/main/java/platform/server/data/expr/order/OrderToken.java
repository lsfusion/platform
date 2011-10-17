package platform.server.data.expr.order;

import platform.server.data.sql.SQLSyntax;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class OrderToken {

    public Set<OrderCalc> next = new HashSet<OrderCalc>(); // где использовался

    public abstract String getSource(Map<OrderToken, String> sources, SQLSyntax syntax);

    public abstract int getLevel();
}
