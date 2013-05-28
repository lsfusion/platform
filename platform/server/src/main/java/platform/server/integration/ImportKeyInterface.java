package platform.server.integration;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.session.Modifier;
import platform.server.session.SinglePropertyTableUsage;

/**
 * User: DAle
 * Date: 31.01.11
 * Time: 16:00
 */

public interface ImportKeyInterface {

    Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier);
}
