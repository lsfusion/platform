package platform.server.integration;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;
import platform.server.session.SingleKeyTableUsage;
import platform.server.session.SinglePropertyTableUsage;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: DAle
 * Date: 27.01.11
 * Time: 18:11
 */

public class ImportProperty <P extends PropertyInterface> {
    private PropertyImplement<ImportKeyInterface, P> implement;
    private ImportFieldInterface importField;

    private PropertyImplement<ImportKeyInterface, P> converter;

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<ImportKeyInterface, P> implement) {
        this.importField = importField;
        this.implement = implement;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<ImportKeyInterface, P> implement, PropertyImplement<ImportKeyInterface, P> converter) {
        this(importField, implement);
        this.converter = converter;
    }

    public PropertyImplement<ImportKeyInterface, P> getProperty() {
        return implement;
    }

    public PropertyImplement<ImportKeyInterface, P> getConverter() {
        return converter;
    }

    public ImportFieldInterface getImportField() {
        return importField;
    }

    Object convertValue(DataSession session, Map<ImportKeyInterface, DataObject> keyValues) throws SQLException {
        Map<P, DataObject> mapping =
                BaseUtils.join(getConverter().mapping, createMapping(getConverter().mapping.values(), keyValues));
        return converter.property.read(session.sql, mapping, session.modifier, session.env);
    }

    void writeValue(DataSession session, Map<ImportKeyInterface, DataObject> keyValues, Object value) throws SQLException {
        Map<P, DataObject> mapping =
                BaseUtils.join(getProperty().mapping, createMapping(getProperty().mapping.values(), keyValues));
        getProperty().property.execute(mapping, session, value, session.modifier);
    }

    private Map<ImportKeyInterface, DataObject> createMapping(Collection<ImportKeyInterface> interfaces, Map<ImportKeyInterface, DataObject> keyValues) {
        Map<ImportKeyInterface, DataObject> mapping = new HashMap<ImportKeyInterface, DataObject>(keyValues);
        for (ImportKeyInterface iface : interfaces) {
            if (!mapping.containsKey(iface)) {
                mapping.put(iface, (DataObject) iface);
            }
        }
        return mapping;
    }

    private static <P> Map<P, Expr> getImplementExprs(Map<P, ImportKeyInterface> mapping, Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Map<ImportField, Expr> importExprs) {
        Map<P, Expr> importKeyExprs = new HashMap<P, Expr>();
        for(Map.Entry<P, ImportKeyInterface> entry : mapping.entrySet())
            importKeyExprs.put(entry.getKey(), entry.getValue().getExpr(importExprs, addedKeys));
        return importKeyExprs;
    }

    public void synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable, Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys) throws SQLException {

        Map<ImportField,Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

        Expr importExpr;
        if (converter != null)
            importExpr = converter.property.getExpr(getImplementExprs(converter.mapping, addedKeys, importExprs), session.modifier);
        else
            importExpr = importField.getExpr(importExprs);

        Map<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        Map<P, Expr> importKeyExprs = getImplementExprs(implement.mapping, addedKeys, importExprs);
        session.execute(implement.property, new PropertyChange<P>(mapKeys, GroupExpr.create(importKeyExprs, importExpr, true, mapKeys)), session.modifier, null, null);
    }
}
