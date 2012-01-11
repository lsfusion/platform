package platform.server.integration;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.session.Modifier;
import platform.server.session.SessionTableUsage;

public interface ImportDeleteInterface {
    Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier);
}
