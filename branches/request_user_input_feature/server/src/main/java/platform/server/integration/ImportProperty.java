package platform.server.integration;

import platform.interop.Compare;
import platform.server.Message;
import platform.server.ThisMessage;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyImplement;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

/**
 * User: DAle
 * Date: 27.01.11
 * Time: 18:11
 */

public class ImportProperty <P extends PropertyInterface> {
    private CalcPropertyImplement<P, ImportKeyInterface> implement;
    private ImportFieldInterface importField;
    private GroupType groupType;

    private CalcPropertyImplement<P, ImportKeyInterface> converter;

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement) {
        this.importField = importField;
        this.implement = implement;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, GroupType groupType) {
        this(importField, implement);
        this.groupType = groupType;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, CalcPropertyImplement<P, ImportKeyInterface> converter) {
        this(importField, implement);
        this.converter = converter;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, CalcPropertyImplement<P, ImportKeyInterface> converter, GroupType groupType) {
        this(importField, implement, converter);
        this.groupType = groupType;
    }

    public CalcPropertyImplement<P, ImportKeyInterface> getProperty() {
        return implement;
    }

    private static <P> Map<P, Expr> getImplementExprs(Map<P, ImportKeyInterface> mapping, Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Map<ImportField, Expr> importExprs, Modifier modifier) {
        Map<P, Expr> importKeyExprs = new HashMap<P, Expr>();
        for(Map.Entry<P, ImportKeyInterface> entry : mapping.entrySet())
            importKeyExprs.put(entry.getKey(), entry.getValue().getExpr(importExprs, addedKeys, modifier));
        return importKeyExprs;
    }

    @Override
    public String toString() {
        return implement.property.toString();
    }

    @Message("message.synchronize.property")
    @ThisMessage
    public DataChanges synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable, Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, boolean replaceNull, boolean replaceEqual) throws SQLException {
        Map<ImportField,Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

        Expr importExpr;
        if (converter != null)
            importExpr = converter.property.getExpr(getImplementExprs(converter.mapping, addedKeys, importExprs, session.modifier), session.modifier);
        else
            importExpr = importField.getExpr(importExprs);

        Map<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        Map<P, Expr> importKeyExprs = getImplementExprs(implement.mapping, addedKeys, importExprs, session.modifier);

        Expr changeExpr = GroupExpr.create(importKeyExprs, importExpr, groupType != null ? groupType : GroupType.ANY, mapKeys);

        Where changeWhere;
        if (replaceNull)
            changeWhere = GroupExpr.create(importKeyExprs, Where.TRUE, mapKeys).getWhere();
        else
            changeWhere = changeExpr.getWhere();

        if (!replaceEqual) {
            changeWhere = changeWhere.and(implement.property.getExpr(mapKeys).compare(changeExpr, Compare.EQUALS).not());
        }

        return ((CalcProperty<P>)implement.property).getDataChanges(new PropertyChange<P>(mapKeys, changeExpr, changeWhere), session.modifier);
    }
}
