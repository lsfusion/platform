package lsfusion.server.integration;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SinglePropertyTableUsage;

import java.sql.SQLException;

/**
 * User: DAle
 * Date: 31.01.11
 * Time: 16:00
 */

public interface ImportKeyInterface {

    Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier) throws SQLException, SQLHandledException;
}
