package platform.server.data.expr.order;

import platform.server.data.sql.SQLSyntax;

import java.util.Map;

public class PartitionParam extends PartitionToken {

    public String getSource(Map<PartitionToken, String> sources, SQLSyntax syntax) {
        return sources.get(this);
    }

    public int getLevel() {
        return 0;
    }
}
