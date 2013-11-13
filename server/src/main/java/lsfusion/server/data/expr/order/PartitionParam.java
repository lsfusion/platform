package lsfusion.server.data.expr.order;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class PartitionParam extends PartitionToken {

    public String getSource(ImMap<PartitionToken, String> sources, SQLSyntax syntax, Type resultType, TypeEnvironment typeEnv) {
        return sources.get(this);
    }

    public int getLevel() {
        return 0;
    }
}
