package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.TableOwner;
import lsfusion.server.data.sql.SQLSyntax;

public class MaterializedQuery {
    public final String tableName;
    public final String mapFields;
    public final int count;
    public final long timeExec;

    // for debug
    public final ImOrderSet<KeyField> keyFields;
    public final ImSet<PropertyField> propFields;

    public final Owner owner;

    public String getParsedString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
        return "(SELECT " + mapFields + " FROM " + syntax.getQueryName(tableName, null, envString, usedRecursion) + ")";
    }

    public static class Owner implements TableOwner {}

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
