package lsfusion.server.integration;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SessionTableUsage;

import java.sql.SQLException;

public interface ImportDeleteInterface {
    Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) throws SQLException, SQLHandledException;
}
