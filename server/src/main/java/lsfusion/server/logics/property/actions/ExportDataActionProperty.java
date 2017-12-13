package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.ExtendContextActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.Modifier;

import java.io.IOException;
import java.sql.SQLException;

import static lsfusion.server.logics.property.derived.DerivedProperty.createSetAction;

public class ExportDataActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private final ImOrderSet<String> fields;
    private ImMap<String, CalcPropertyInterfaceImplement<I>> exprs;
    protected final CalcPropertyInterfaceImplement<I> where;
    
    private final LCP targetProp;

    public ExportDataActionProperty(LocalizedString caption,
                                    ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, 
                                    ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, CalcPropertyInterfaceImplement<I> where, LCP targetProp) {
        super(caption, innerInterfaces, mapInterfaces);

        this.fields = fields;
        this.exprs = exprs;
        this.where = where;
        this.targetProp = targetProp;

//        assert mapInterfaces.getSet().merge(writeTo.getInterfaces()).equals(innerInterfaces);

        finalizeInit();
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

    // пока не будем, так как и ExportActionProperty не перегружает
//    @Override
//    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
//        if(where!=null)
//            return getUsedProps(writeFrom, where);
//        return getUsedProps(writeFrom);
//    }

    @Override
    protected FlowResult executeExtend(final ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, final ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
        final Modifier modifier = context.getModifier();

        final Query<I, String> query = getQuery(innerKeys, innerExprs, modifier);
        ImList<ImMap<String, Object>> rows = query.execute(context).valuesList();

        try {
            targetProp.change(BaseUtils.mergeFileAndExtension(JDBCTable.serialize(fields, new Type.Getter<String>() { // можно было бы типы заранее высчитать, но могут быть значения сверху и тогда их тип будет неизвестен заранее
                public Type getType(String value) {
                    return query.getPropertyType(value);
                }
            }, rows), "jdbc".getBytes()), context);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return FlowResult.FINISH;
    }

    private Query<I, String> getQuery(ImRevMap<I, KeyExpr> innerKeys, final ImMap<I, ? extends Expr> innerExprs, final Modifier modifier) throws SQLException, SQLHandledException {
        // если не хватает ключей надо or добавить, так чтобы кэширование работало
        ImSet<I> extInterfaces = innerInterfaces.remove(mapInterfaces.valuesSet());
        CalcPropertyInterfaceImplement<I> changeWhere = (where == null && extInterfaces.isEmpty()) || (where != null && where.mapIsFull(extInterfaces)) ?
                (where == null ? DerivedProperty.<I>createTrue() : where) : getFullProperty();

        return new Query<>(innerKeys, exprs.mapValuesEx(new GetExValue<Expr, CalcPropertyInterfaceImplement<I>, SQLException, SQLHandledException>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) throws SQLException, SQLHandledException {
                return value.mapExpr(innerExprs, modifier);
            }}), changeWhere.mapExpr(innerExprs, modifier).getWhere());
    }

    public static <I extends PropertyInterface> CalcPropertyMapImplement<?, I> getFullProperty(ImSet<I> innerInterfaces, CalcPropertyInterfaceImplement<I> where, ImCol<CalcPropertyInterfaceImplement<I>> exprs) {
        CalcPropertyMapImplement<?, I> result = DerivedProperty.createUnion(innerInterfaces, exprs.mapColValues(new GetValue<CalcPropertyInterfaceImplement<I>, CalcPropertyInterfaceImplement<I>>() {
            public CalcPropertyInterfaceImplement<I> getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return DerivedProperty.createNotNull(value);
            }}).toList());
        if(where!=null) 
            result = DerivedProperty.createAnd(innerInterfaces, where, result);
        return result;
    }

    @IdentityInstanceLazy
    private CalcPropertyMapImplement<?, I> getFullProperty() {
        return getFullProperty(innerInterfaces, where, exprs.values());
    }

    protected CalcPropertyMapImplement<?, I> calcGroupWhereProperty() {
        return getFullProperty();
    }
}
