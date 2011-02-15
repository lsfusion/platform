package platform.server.integration;

import platform.server.data.expr.Expr;
import platform.server.logics.DataObject;

import java.util.Map;

/**
 * User: DAle
 * Date: 03.02.11
 * Time: 18:25
 */

public interface ImportFieldInterface {
    DataObject getDataObject(ImportTable.Row row);

    Expr getExpr(Map<ImportField, ? extends Expr> importKeys);
}
