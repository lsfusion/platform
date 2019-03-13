package lsfusion.server.physics.dev.integration.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.logics.action.session.Modifier;
import lsfusion.server.logics.action.session.SessionTableUsage;

import java.sql.SQLException;

public interface ImportDeleteInterface {
    Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) throws SQLException, SQLHandledException;
}
