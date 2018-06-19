package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.AbstractType;
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
import java.util.List;

public abstract class ExportDataActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private final String extension;
    protected final ImOrderSet<String> fields;
    private ImMap<String, CalcPropertyInterfaceImplement<I>> exprs;
    private ImMap<String, Type> types;
    private final CalcPropertyInterfaceImplement<I> where;
    private ImOrderMap<String, Boolean> orders;

    private final LCP<?> targetProp;

    protected abstract byte[] getFile(final Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException;

    public ExportDataActionProperty(LocalizedString caption, String extension,
                                    ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                    ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs,
                                    ImMap<String, Type> types, CalcPropertyInterfaceImplement<I> where, ImOrderMap<String, Boolean> orders, LCP targetProp) {
        super(caption, innerInterfaces, mapInterfaces);
        this.extension = extension;
        this.fields = fields;
        this.exprs = exprs;
        this.types = types;
        this.where = where;
        this.orders = orders;
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
        ImList<ImMap<String, Object>> rows = query.execute(context, orders, 0).valuesList();

        try {
            // можно было бы типы заранее высчитать, но могут быть значения сверху и тогда их тип будет неизвестен заранее
            targetProp.change(BaseUtils.mergeFileAndExtension(getFile(query, rows, new Type.Getter<String>() {
                public Type getType(String value) {
                    Type propertyType = query.getPropertyType(value);
                    if(propertyType == null) {
                        propertyType = types.get(value);
                        if(propertyType == null)
                            propertyType = AbstractType.getUnknownTypeNull();
                    }
                    return propertyType;
                }
            }), extension.getBytes()), context);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return FlowResult.FINISH;
    }

    protected Query<I, String> getQuery(ImRevMap<I, KeyExpr> innerKeys, final ImMap<I, ? extends Expr> innerExprs, final Modifier modifier) throws SQLException, SQLHandledException {
        // если не хватает ключей надо or добавить, так чтобы кэширование работало
        ImSet<I> extInterfaces = innerInterfaces.remove(mapInterfaces.valuesSet());
        CalcPropertyInterfaceImplement<I> changeWhere = (where == null && extInterfaces.isEmpty()) || (where != null && where.mapIsFull(extInterfaces)) ?
                (where == null ? DerivedProperty.<I>createTrue() : where) : getFullProperty();

        return new Query<>(innerKeys, exprs.mapValuesEx(new GetExValue<Expr, CalcPropertyInterfaceImplement<I>, SQLException, SQLHandledException>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) throws SQLException, SQLHandledException {
                return value.mapExpr(innerExprs, modifier);
            }
        }), changeWhere.mapExpr(innerExprs, modifier).getWhere());
    }

    public static <I extends PropertyInterface> CalcPropertyMapImplement<?, I> getFullProperty(ImSet<I> innerInterfaces, CalcPropertyInterfaceImplement<I> where, ImCol<CalcPropertyInterfaceImplement<I>> exprs) {
        CalcPropertyMapImplement<?, I> result = DerivedProperty.createUnion(innerInterfaces, exprs.mapColValues(new GetValue<CalcPropertyInterfaceImplement<I>, CalcPropertyInterfaceImplement<I>>() {
            public CalcPropertyInterfaceImplement<I> getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return DerivedProperty.createNotNull(value);
            }
        }).toList());
        if (where != null)
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

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        if(where!=null)
            return getUsedProps(exprs.values(), where);
        return getUsedProps(exprs.values());
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(targetProp.property);
    }
}
