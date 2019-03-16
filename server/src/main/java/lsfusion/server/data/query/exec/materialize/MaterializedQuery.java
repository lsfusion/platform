package lsfusion.server.data.query.exec.materialize;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;

public class MaterializedQuery {
    public final String tableName;
    public final String mapFields;
    public final int count;
    public final long timeExec;

    // for debug
    public final ImOrderSet<KeyField> keyFields;
    public final ImSet<PropertyField> propFields;

    public final Owner owner;

    public String getParsedString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion, EnsureTypeEnvironment typeEnv) {
        return "(SELECT " + mapFields + " FROM " + syntax.getQueryName(tableName, null, envString, usedRecursion, typeEnv) + ")";
    }

    public static class Owner implements TableOwner {
        @Override
        public String getDebugInfo() {
            return "matquery";
        }
    }

    public MaterializedQuery(String tableName, String mapFields, ImOrderSet<KeyField> keyFields, ImSet<PropertyField> propFields, int count, long timeExec, Owner owner) {
        this.tableName = tableName;
        this.mapFields = mapFields;
        this.count = count;
        this.timeExec = timeExec;
        this.owner = owner;

        this.keyFields = keyFields;
        this.propFields = propFields;
    }
}
