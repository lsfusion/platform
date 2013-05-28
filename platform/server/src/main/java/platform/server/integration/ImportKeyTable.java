package platform.server.integration;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Modifier;
import platform.server.session.SessionTableUsage;

public class ImportKeyTable<P extends PropertyInterface> implements ImportDeleteInterface {
    ImportKey<P> key;
    SessionTableUsage<String, ImportField> table;

    public ImportKeyTable(ImportKey<P> key, SessionTableUsage<String, ImportField> table) {
        this.key = key;
        this.table = table;
    }

    @Override
    public Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) {
        return key.getDeleteExpr(table, intraKeyExpr, modifier);
    }
}
