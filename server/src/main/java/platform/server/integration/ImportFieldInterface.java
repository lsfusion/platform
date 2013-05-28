package platform.server.integration;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;

/**
 * User: DAle
 * Date: 03.02.11
 * Time: 18:25
 */

public interface ImportFieldInterface {
    DataObject getDataObject(ImportTable.Row row);

    Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys);

    Type getType();
}
