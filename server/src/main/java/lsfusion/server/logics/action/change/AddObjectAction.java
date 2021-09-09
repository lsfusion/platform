package lsfusion.server.logics.action.change;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.data.PropertyOrderSet;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.ExtendContextAction;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.table.SinglePropertyTableUsage;
import lsfusion.server.logics.classes.user.AbstractCustomClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectClass;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapAdd;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class AddObjectAction<T extends PropertyInterface, I extends PropertyInterface> extends ExtendContextAction<I> {

    protected final CustomClass valueClass; // обозначает класс объекта, который нужно добавить
    private final boolean autoSet;

    protected PropertyMapImplement<T, I> where;
    private PropertyMapImplement<?, I> result; // только extend интерфейсы

    private final ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final boolean ordersNotNull;

    public <T extends PropertyInterface> AddObjectAction(CustomClass valueClass, Property<T> result, boolean autoSet) {
        this(valueClass, SetFact.EMPTY(), SetFact.EMPTYORDER(), null, result!=null ? new PropertyMapImplement<>(result) : null, MapFact.EMPTYORDER(), false, autoSet);
    }

    public AddObjectAction(CustomClass valueClass, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, PropertyMapImplement<T, I> where, PropertyMapImplement<?, I> result, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, boolean autoSet) {
        super(LocalizedString.create("{logics.add}"), innerInterfaces, mapInterfaces);
        
        this.valueClass = valueClass;
        
        this.autoSet = autoSet;
        
        this.where = where;
        this.result = result;
        
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
        
        assert where==null || !needDialog();
        
        assert where==null || !autoSet;

        assert where==null || result==null || innerInterfaces.containsAll(where.mapping.valuesSet().merge(result.mapping.valuesSet()));
    }

    // not sure that we gonna support this branch
    @Deprecated
    protected boolean needDialog() {
        return valueClass instanceof AbstractCustomClass;  // || (forceDialog && valueClass.hasChildren())
    }

    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        if(where==null)
            return MapFact.EMPTY();
        return getUsedProps(where);
    }

    @Override
    public ImMap<Property, Boolean> aspectChangeExtProps() {
        ImMap<Property, Boolean> result = getChangeExtProps(valueClass, needDialog());
        if(this.result!=null)
            result = result.merge(this.result.property.getChangeProps().toMap(false), addValue);
        return result;
    }
    
    public static ImMap<Property, Boolean> getChangeExtProps(CustomClass valueClass, boolean needDialog) {
        if(valueClass == null) // добавление unknown, используется в агрегациях
            return MapFact.EMPTY();
        
        MExclMap<Property, Boolean> mResult = MapFact.mExclMap();
        mResult.exclAddAll(valueClass.getParentSetProps().toMap(false));
        
        if(needDialog)
            mResult.exclAddAll(valueClass.getUpDataProps().toMap(false)); // set
        else
            mResult.exclAdd(((ConcreteCustomClass)valueClass).dataProperty, false); // set
        
        mResult.exclAdd(valueClass.getBaseClass().getObjectClassProperty(), false);
        return mResult.immutable();
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        if(where==null && !needDialog())
            return new AsyncMapAdd<>(valueClass);
        return null;
    }

    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
        ObjectClass concreteClass = getConcreteClass(context);

        if(concreteClass !=null)
            executeRead(context, innerKeys, innerExprs, (ConcreteCustomClass) concreteClass);

        return FlowResult.FINISH;
    }

    private ObjectClass getConcreteClass(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectClass readClass;
        if (needDialog()) {
            ObjectValue objectValue = context.requestUserClass(valueClass, valueClass, true);
            if (!(objectValue instanceof DataObject)) // cancel
                readClass = null;
            else
                readClass = valueClass.getBaseClass().findClassID((Long) ((DataObject) objectValue).object);
        } else
            readClass = valueClass;
        return readClass;
    }

    protected void executeRead(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, Expr> innerExprs, ConcreteCustomClass readClass) throws SQLException, SQLHandledException {
        SinglePropertyTableUsage<I> addedTable = null;
        DataSession session = context.getSession();
        try {
            PropertyChange<I> resultChange;
            if(where==null) { // оптимизация, один объект добавляем
                DataObject addObject = context.addObject(readClass, autoSet);
                resultChange = new PropertyChange<>(addObject);
            } else {
                if(result!=null)
                    session.dropChanges((DataProperty) result.property);
    
                final Modifier modifier = context.getModifier();
                Where exprWhere = where.mapExpr(innerExprs, modifier).getWhere();
                if(exprWhere.isFalse()) // оптимизация, важна так как во многих event'ах может учавствовать
                    return;
    
                final ImMap<I, ? extends Expr> fInnerExprs = PropertyChange.simplifyExprs(innerExprs, exprWhere);
                ImOrderMap<Expr, Boolean> orderExprs = orders.mapMergeOrderKeysEx((ThrowingFunction<PropertyInterfaceImplement<I>, Expr, SQLException, SQLHandledException>) value -> value.mapExpr(fInnerExprs, modifier));
    
                addedTable = context.addObjects("addobjap", readClass, new PropertyOrderSet<>(innerKeys, exprWhere, orderExprs, ordersNotNull));
                resultChange = SinglePropertyTableUsage.getChange(addedTable);
            }
    
            if(result != null)
                result.change(context.getEnv(), resultChange);
        } finally {
            if(addedTable!=null)
                addedTable.drop(session.sql, session.getOwner());
        }
    }

    protected PropertyMapImplement<?, I> calcGroupWhereProperty() {
        if(where==null)
            return PropertyFact.createTrue();
        return where;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type.isChange())
            return true;
        return super.hasFlow(type);
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // need this for class breakpoints
    }
}
