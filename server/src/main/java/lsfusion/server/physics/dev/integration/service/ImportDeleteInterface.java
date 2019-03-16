package lsfusion.server.physics.dev.integration.service;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.table.SessionTableUsage;

import java.sql.SQLException;

public interface ImportDeleteInterface {
    Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) throws SQLException, SQLHandledException;
}
