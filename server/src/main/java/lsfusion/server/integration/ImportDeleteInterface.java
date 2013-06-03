package lsfusion.server.integration;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SessionTableUsage;

public interface ImportDeleteInterface {
    Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier);
}
