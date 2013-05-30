package platform.server.integration;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
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
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

import java.sql.SQLException;

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

    private static <P> ImMap<P, Expr> getImplementExprs(final ImMap<P, ImportKeyInterface> mapping, final ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, final ImMap<ImportField, Expr> importExprs, final Modifier modifier) {
        return mapping.mapValues(new GetValue<Expr, ImportKeyInterface>() {
            public Expr getMapValue(ImportKeyInterface value) {
                return value.getExpr(importExprs, addedKeys, modifier);
            }});
    }

    @Override
    public String toString() {
        return implement.property.toString();
    }

    @Message("message.synchronize.property")
    @ThisMessage
    public DataChanges synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, boolean replaceNull, boolean replaceEqual) throws SQLException {
        ImMap<ImportField,Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

        Expr importExpr;
        if (converter != null)
            importExpr = converter.property.getExpr(getImplementExprs(converter.mapping, addedKeys, importExprs, session.getModifier()), session.getModifier());
        else
            importExpr = importField.getExpr(importExprs);

        ImRevMap<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        ImMap<P, Expr> importKeyExprs = getImplementExprs(implement.mapping, addedKeys, importExprs, session.getModifier());

        Expr changeExpr = GroupExpr.create(importKeyExprs, importExpr, groupType != null ? groupType : GroupType.ANY, mapKeys);

        Where changeWhere;
        if (replaceNull)
            changeWhere = GroupExpr.create(importKeyExprs, Where.TRUE, mapKeys).getWhere();
        else
            changeWhere = changeExpr.getWhere();

        if (!replaceEqual) {
            changeWhere = changeWhere.and(implement.property.getExpr(mapKeys).compare(changeExpr, Compare.EQUALS).not());
        }

        return ((CalcProperty<P>)implement.property).getDataChanges(new PropertyChange<P>(mapKeys, changeExpr, changeWhere), session.getModifier());
    }
}
