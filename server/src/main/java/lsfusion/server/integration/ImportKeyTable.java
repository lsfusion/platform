package lsfusion.server.integration;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SessionTableUsage;

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
