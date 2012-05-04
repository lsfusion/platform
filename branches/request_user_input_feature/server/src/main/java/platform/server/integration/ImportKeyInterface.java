package platform.server.integration;

import platform.server.data.expr.Expr;
import platform.server.session.Modifier;
import platform.server.session.SinglePropertyTableUsage;

import java.util.Map;

/**
 * User: DAle
 * Date: 31.01.11
 * Time: 16:00
 */

public interface ImportKeyInterface {

    Expr getExpr(Map<ImportField, ? extends Expr> importKeys, Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier);
}
