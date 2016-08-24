package lsfusion.server.logics.linear;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.LogFormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.*;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.List;

import static lsfusion.server.logics.PropertyUtils.mapCalcImplement;
import static lsfusion.server.logics.PropertyUtils.readCalcImplements;

public class LCP<T extends PropertyInterface> extends LP<T, CalcProperty<T>> {

    public LCP(CalcProperty<T> property) {
        super(property);
    }

    public LCP(CalcProperty<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public Object read(FormInstance form, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.read(form, getMapValues(objects));
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.read(session, getMapValues(objects), modifier, env);
    }

    public Object read(ExecutionContext context, ObjectValue... objects) throws SQLException, SQLHandledException {
        return read(context.getSession().sql, context.getModifier(), context.getQueryEnv(), objects);
    }

    public Object read(DataSession session, ObjectValue... objects) throws SQLException, SQLHandledException {
        return read(session.sql, session.getModifier(), session.env, objects);
    }

    public ObjectValue readClasses(DataSession session, Modifier modifier, QueryEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        ImMap<T, ObjectValue> mapValues = getMapValues(objects);
        return property.readClasses(session, mapValues, modifier, env);
    }

    public ObjectValue readClasses(ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        return readClasses(context.getSession(), context.getModifier(), context.getQueryEnv(), objects);
    }

    public ObjectValue readClasses(DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        return readClasses(session, session.getModifier(), session.env, objects);
    }

    // execute'ы без Form'
    public void change(Object value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, (ExecutionEnvironment)session, objects);
    }

    public void change(ObjectValue value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, context.getEnv(), objects);
    }

    public void change(Object value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, context.getEnv(), objects);
    }

    public void change(Object value, ExecutionContext context, ImMap<T, DataObject> keys) throws SQLException, SQLHandledException {
        change(value, context.getEnv(), keys);
    }

    public void change(ObjectValue value, ExecutionEnvironment env, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, env, getMapDataValues(objects));
    }

    public void change(Object value, ExecutionEnvironment env, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, env, getMapDataValues(objects));
    }

    public void change(ObjectValue value, ExecutionEnvironment env, ImMap<T, DataObject> keys) throws SQLException, SQLHandledException {
        property.change(keys, env, value);
    }

    public void change(Object value, ExecutionEnvironment env, ImMap<T, DataObject> keys) throws SQLException, SQLHandledException {
        //отдельно обрабатываем false-значения: используем null вместо false
        if (value instanceof Boolean && !(Boolean)value) {
            value = null;
        }
        property.change(keys, env, value);
    }

    public void makeLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM) {
        makeLoggable(ownerModule, systemEventsLM, false);
    }

    public void makeLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM, boolean lazyInit) {
        setupLoggable(ownerModule, systemEventsLM, lazyInit);
        property.loggable = true;
    }

    public void makeUserLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM) {
        setupLoggable(ownerModule, systemEventsLM, false);
        property.loggable = true;
    }

    private void setupLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM, boolean lazyInit) {
        if (property.getLogProperty() == null) {
            property.setLogProperty(ownerModule.addLProp(systemEventsLM, this));
        }
        if (property.getLogFormProperty() == null) {
            LogFormEntity logFormEntity = new LogFormEntity(null,
                                                            ServerResourceBundle.getString("logics.property.log.form"),
                                                            this, property.getLogProperty(), systemEventsLM, lazyInit);
            systemEventsLM.addFormEntity(logFormEntity);
            property.setLogFormProperty(ownerModule.addMFAProp(ServerResourceBundle.getString("logics.property.log.action"), logFormEntity, logFormEntity.params));
        }
    }

    public <D extends PropertyInterface> void setEventChange(LCP<D> valueProperty, int whereNum, Object... params) {

        ImList<CalcPropertyInterfaceImplement<T>> defImplements = readCalcImplements(listInterfaces, params);

        property.setEventChange(LogicsModule.mapCalcListImplement(valueProperty, listInterfaces),
                BaseUtils.<ImList<CalcPropertyMapImplement<?, T>>>immutableCast(defImplements.subList(0, whereNum)),
                BaseUtils.<ImList<CalcPropertyMapImplement<?, T>>>immutableCast(defImplements.subList(whereNum, defImplements.size())).getCol());
    }

    public <D extends PropertyInterface> void setEventChange(LogicsModule lm, boolean action, Object... params) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        property.setEventChange(lm, action, listImplements.get(0), (CalcPropertyMapImplement<PropertyInterface, T>) listImplements.get(1));
    }

    public void addOperand(boolean hasWhen, List<ResolveClassSet> signature, Version version, Object... params) {
        ImList<CalcPropertyInterfaceImplement<T>> readImplements = readCalcImplements(listInterfaces, params);
        CalcPropertyInterfaceImplement<UnionProperty.Interface> operand = (CalcPropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(0);
        if(hasWhen)
            ((CaseUnionProperty)property).addCase((CalcPropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(1), operand, version);
        else {
//            if(((CaseUnionProperty) property).getAbstractType() == CaseUnionProperty.Type.MULTI) {
//                AbstractCase.cntexpl = AbstractCase.cntexpl + 1;
//                if(operand instanceof CalcPropertyMapImplement) {
//                    if(BaseUtils.nullEquals(((CalcPropertyMapImplement)operand).property.getName(), property.getName()))
//                        AbstractCase.cntexplname = AbstractCase.cntexplname + 1;
//                }
//            }
            ((CaseUnionProperty) property).addOperand((CalcPropertyMapImplement<?, UnionProperty.Interface>) operand, signature, version);
        }
    }

    public ImRevMap<T, KeyExpr> getMapKeys() {
        return property.getMapKeys();
    }

    public Expr getExpr(Modifier modifier, final Expr... exprs) throws SQLException, SQLHandledException {
        return property.getExpr(getMap(exprs),modifier);
    }

    public Expr getExpr(final Expr... exprs) {
        return property.getExpr(getMap(exprs));
    }

    public <U> CalcPropertyImplement<T, U> getMapping(U... mapping) {
        return new CalcPropertyImplement<>(property, getMap(mapping));
    }
    public <U extends PropertyInterface> CalcPropertyMapImplement<T, U> getImplement(U... mapping) {
        return new CalcPropertyMapImplement<>(property, getRevMap(mapping));
    }

    public PropertyChange<T> getChange(Expr expr, Where where, KeyExpr... keys) {
        return new PropertyChange<>(getRevMap(keys), expr, where);
    }

    public void setAutoset(boolean autoset) {
        assert property.interfaces.size()==1;
        property.autoset = autoset;
    }

    public ValueClass[] getInterfaceClasses(ClassType classType) {
        return property.getInterfaceClasses(listInterfaces, classType);
    }
    
    public LAP getEditAction(String editActionSID) {
        return property.getEditAction(editActionSID).createLP(listInterfaces);
    }

    public <A extends PropertyInterface> void setEditAction(String editActionSID, LAP<A> editAction) {
        property.setEditAction(editActionSID, new ActionPropertyMapImplement<>(editAction.property, editAction.getRevMap(listInterfaces)));
    }
    
    public LCP<T> getOld() {
        return new LCP<>(property.getOld(PrevScope.DB), listInterfaces);
    }
    
    public ResolveClassSet getResolveClassSet(List<ResolveClassSet> classes) {
        return property.getResolveClassSet(listInterfaces.mapList(ListFact.fromJavaList(classes)));    
    }
}
