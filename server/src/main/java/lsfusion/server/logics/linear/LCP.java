package lsfusion.server.logics.linear;

import lsfusion.base.BaseUtils;
import lsfusion.base.FileData;
import lsfusion.base.RawFileData;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.LogFormEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.SystemEventsLogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChange;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import static lsfusion.server.logics.PropertyUtils.readCalcImplements;

public class LCP<T extends PropertyInterface> extends LP<T, CalcProperty<T>> {

    public LCP(CalcProperty<T> property) {
        super(property);
    }

    public LCP(CalcProperty<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public Object read(ExecutionEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.read(env, getMapValues(objects));
    }

    public ImMap<ImList<Object>, Object> readAll(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return property.readAll(env).mapKeys(new GetValue<ImList<Object>, ImMap<T, Object>>() {
            public ImList<Object> getMapValue(ImMap<T, Object> value) {
                return listInterfaces.mapList(value);
            }
        });
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

    public ObjectValue readClasses(ExecutionContext context, ObjectValue... objects) throws SQLException, SQLHandledException {
        return readClasses(context.getEnv(), objects);
    }

    public ObjectValue readClasses(ExecutionEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return readClasses(env.getSession(), env.getModifier(), env.getQueryEnv(), objects);
    }

    public ObjectValue readClasses(DataSession session, ObjectValue... objects) throws SQLException, SQLHandledException {
        return readClasses(session, session.getModifier(), session.env, objects);
    }

    // execute'ы без Form'
    public void change(Object value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, (ExecutionEnvironment)session, objects);
    }
    // execute'ы из контекста
    public void change(Object value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, context.getEnv(), objects);
    }

    // с явными типами
    public void change(Integer value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(Integer value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(Long value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(Long value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(Boolean value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(Boolean value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(Date value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(Date value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(java.sql.Timestamp value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(java.sql.Timestamp value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(BigDecimal value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(BigDecimal value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(RawFileData value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(RawFileData value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(FileData value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(FileData value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(String value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(String value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(java.sql.Time value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(java.sql.Time value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }

    // ObjectValue
    public void change(ObjectValue value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, (ExecutionEnvironment) session, objects);
    }
    public void change(ObjectValue value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change(value, context.getEnv(), objects);
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
        setupLoggable(ownerModule, systemEventsLM);
        property.setLoggable(true);
    }

    public void makeUserLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM) {
        setupLoggable(ownerModule, systemEventsLM);
        property.setLoggable(true);
    }

    private void setupLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM) {
        if (property.getLogValueProperty() == null) {
            LCP logValueProperty = ownerModule.addLProp(systemEventsLM, this);
            LCP logDropProperty = ownerModule.addLDropProp(systemEventsLM, this);
            
            property.setLogValueProperty(logValueProperty);
            property.setLogWhereProperty(ownerModule.addLWhereProp(logValueProperty, logDropProperty));
        }
        if (property.getLogFormProperty() == null) {
            LogFormEntity logFormEntity = new LogFormEntity(null,
                                                            LocalizedString.create("{logics.property.log.form}"),
                                                            this, property.getLogValueProperty(), property.getLogWhereProperty(), systemEventsLM);
            systemEventsLM.addFormEntity(logFormEntity);
            property.setLogFormProperty(ownerModule.addMFAProp(LocalizedString.create("{logics.property.log.action}"), logFormEntity, logFormEntity.params, true));
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

    public LCP<T> getOld() {
        return new LCP<>(property.getOld(PrevScope.DB), listInterfaces);
    }
    
    public ResolveClassSet getResolveClassSet(List<ResolveClassSet> classes) {
        return property.getResolveClassSet(listInterfaces.mapList(ListFact.fromJavaList(classes)));    
    }
}
