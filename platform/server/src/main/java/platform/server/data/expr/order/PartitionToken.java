package platform.server.data.expr.order;

import platform.server.data.sql.SQLSyntax;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class PartitionToken {

    public Set<PartitionCalc> next = new HashSet<PartitionCalc>(); // где использовался

    public abstract String getSource(Map<PartitionToken, String> sources, SQLSyntax syntax);

    public abstract int getLevel();
}
