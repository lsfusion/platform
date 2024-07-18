package lsfusion.server.physics.dev.integration.service;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.change.modifier.SessionModifier;
import lsfusion.server.logics.action.session.table.SingleKeyTableUsage;
import lsfusion.server.logics.action.session.table.SinglePropertyTableUsage;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class ImportProperty <P extends PropertyInterface> {
    private PropertyImplement<P, ImportKeyInterface> implement;
    private ImportFieldInterface importField;
    private GroupType groupType;
    private boolean replaceOnlyNull;

    private PropertyImplement<P, ImportKeyInterface> converter;

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement) {
        this.importField = importField;
        this.implement = implement;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement, boolean replaceOnlyNull) {
        this.importField = importField;
        this.implement = implement;
        this.replaceOnlyNull = replaceOnlyNull;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement, GroupType groupType) {
        this(importField, implement);
        this.groupType = groupType;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement, GroupType groupType, boolean replaceOnlyNull) {
        this(importField, implement, replaceOnlyNull);
        this.groupType = groupType;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement, PropertyImplement<P, ImportKeyInterface> converter) {
        this(importField, implement);
        this.converter = converter;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement, PropertyImplement<P, ImportKeyInterface> converter, boolean replaceOnlyNull) {
        this(importField, implement, replaceOnlyNull);
        this.converter = converter;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement, PropertyImplement<P, ImportKeyInterface> converter, GroupType groupType) {
        this(importField, implement, converter);
        this.groupType = groupType;
    }

    public ImportProperty(ImportFieldInterface importField, PropertyImplement<P, ImportKeyInterface> implement, PropertyImplement<P, ImportKeyInterface> converter, GroupType groupType, boolean replaceOnlyNull) {
        this(importField, implement, converter, replaceOnlyNull);
        this.groupType = groupType;
    }

    public PropertyImplement<P, ImportKeyInterface> getProperty() {
        return implement;
    }

    private static <P> ImMap<P, Expr> getImplementExprs(final ImMap<P, ImportKeyInterface> mapping, final ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, final ImMap<ImportField, Expr> importExprs, final Modifier modifier) throws SQLException, SQLHandledException {
        return mapping.mapValuesEx((ThrowingFunction<ImportKeyInterface, Expr, SQLException, SQLHandledException>) value -> value.getExpr(importExprs, addedKeys, modifier));
    }

    @Override
    public String toString() {
        return implement.property.toString();
    }

    @StackMessage("{message.synchronize.property}")
    @ThisMessage
    public DataChanges synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, boolean replaceNull, boolean replaceEqual) throws SQLException, SQLHandledException {
        ImMap<ImportField,Expr> importExprs = importTable.getExprs();

        Expr importExpr;
        SessionModifier modifier = session.getModifier();
        Type importType;
        if (converter != null) {
            importExpr = converter.property.getExpr(getImplementExprs(converter.mapping, addedKeys, importExprs, modifier), modifier);
            importType = converter.property.getType();
        } else {
            importExpr = importField.getExpr(importExprs);
            importType = importField.getType();
        }

        ImRevMap<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        ImMap<P, Expr> importKeyExprs = getImplementExprs(implement.mapping, addedKeys, importExprs, modifier);

        Expr changeExpr = GroupExpr.create(importKeyExprs, importExpr, groupType != null ? groupType : GroupType.CHANGE(importType), mapKeys);

        Where changeWhere;
        if (replaceNull)
            changeWhere = GroupExpr.create(importKeyExprs, Where.TRUE(), mapKeys).getWhere();
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
