package platform.server.data.expr.order;

import platform.server.data.sql.SQLSyntax;

import java.util.Map;
import java.util.Set;

public class OrderParam extends OrderToken {

    public String getSource(Map<OrderToken, String> sources, SQLSyntax syntax) {
        return sources.get(this);
    }

    public int getLevel() {
        return 0;
    }
}
