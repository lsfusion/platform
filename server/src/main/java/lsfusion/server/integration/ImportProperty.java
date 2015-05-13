package lsfusion.server.integration;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.interop.Compare;
import lsfusion.server.Message;
import lsfusion.server.ThisMessage;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.property.CalcPropertyImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.*;

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
    private boolean replaceOnlyNull;

    private CalcPropertyImplement<P, ImportKeyInterface> converter;

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement) {
        this.importField = importField;
        this.implement = implement;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, boolean replaceOnlyNull) {
        this.importField = importField;
        this.implement = implement;
        this.replaceOnlyNull = replaceOnlyNull;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, GroupType groupType) {
        this(importField, implement);
        this.groupType = groupType;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, GroupType groupType, boolean replaceOnlyNull) {
        this(importField, implement, replaceOnlyNull);
        this.groupType = groupType;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, CalcPropertyImplement<P, ImportKeyInterface> converter) {
        this(importField, implement);
        this.converter = converter;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, CalcPropertyImplement<P, ImportKeyInterface> converter, boolean replaceOnlyNull) {
        this(importField, implement, replaceOnlyNull);
        this.converter = converter;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, CalcPropertyImplement<P, ImportKeyInterface> converter, GroupType groupType) {
        this(importField, implement, converter);
        this.groupType = groupType;
    }

    public ImportProperty(ImportFieldInterface importField, CalcPropertyImplement<P, ImportKeyInterface> implement, CalcPropertyImplement<P, ImportKeyInterface> converter, GroupType groupType, boolean replaceOnlyNull) {
        this(importField, implement, converter, replaceOnlyNull);
        this.groupType = groupType;
    }

    public CalcPropertyImplement<P, ImportKeyInterface> getProperty() {
        return implement;
    }

    private static <P> ImMap<P, Expr> getImplementExprs(final ImMap<P, ImportKeyInterface> mapping, final ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, final ImMap<ImportField, Expr> importExprs, final Modifier modifier) throws SQLException, SQLHandledException {
        return mapping.mapValuesEx(new GetExValue<Expr, ImportKeyInterface, SQLException, SQLHandledException>() {
            public Expr getMapValue(ImportKeyInterface value) throws SQLException, SQLHandledException {
                return value.getExpr(importExprs, addedKeys, modifier);
            }
        });
    }

    @Override
    public String toString() {
        return implement.property.toString();
    }

    @Message("message.synchronize.property")
    @ThisMessage
    public DataChanges synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, boolean replaceNull, boolean replaceEqual) throws SQLException, SQLHandledException {
        ImMap<ImportField,Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

        Expr importExpr;
        SessionModifier modifier = session.getModifier();
        if (converter != null)
            importExpr = converter.property.getExpr(getImplementExprs(converter.mapping, addedKeys, importExprs, modifier), modifier);
        else
            importExpr = importField.getExpr(importExprs);

        ImRevMap<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        ImMap<P, Expr> importKeyExprs = getImplementExprs(implement.mapping, addedKeys, importExprs, modifier);

        Expr changeExpr = GroupExpr.create(importKeyExprs, importExpr, groupType != null ? groupType : GroupType.ANY, mapKeys);

        Where changeWhere;
        if (replaceNull)
            changeWhere = GroupExpr.create(importKeyExprs, Where.TRUE, mapKeys).getWhere();
        else
            changeWhere = changeExpr.getWhere();

        if (!replaceEqual) {
            changeWhere = changeWhere.and(implement.property.getExpr(mapKeys, modifier).compare(changeExpr, Compare.EQUALS).not());
        }
        
        if(replaceOnlyNull) {
            changeWhere = changeWhere.and(implement.property.getExpr(mapKeys, modifier).getWhere().not());
        }

        return (implement.property).getDataChanges(new PropertyChange<>(mapKeys, changeExpr, changeWhere), modifier);
    }
}
