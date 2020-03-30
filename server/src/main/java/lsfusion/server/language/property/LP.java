package lsfusion.server.language.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.form.LogFormEntity;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.readCalcImplements;

public class LP<T extends PropertyInterface> extends LAP<T, Property<T>> {

    public Property<T> property;

    public Property<T> getActionOrProperty() {
        return property;
    }

    public LP(Property<T> property) {
        super(property);
        this.property = property;
    }

    public LP(Property<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
        this.property = property;
    }

    public Object read(ExecutionEnvironment env, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.read(env, getMapValues(objects));
    }

    public ImMap<ImList<Object>, Object> readAll(ExecutionContext context) throws SQLException, SQLHandledException {
        return readAll(context.getEnv());
    }
    public ImMap<ImList<Object>, Object> readAll(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return property.readAll(env).mapKeys(value -> listInterfaces.mapList(value));
    }
    public ImMap<ImList<DataObject>, DataObject> readAllClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return property.readAllClasses(env).mapKeys(value -> listInterfaces.mapList(value));
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
    public void change(LocalDate value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(LocalDate value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, context, objects);
    }
    public void change(LocalDateTime value, DataSession session, DataObject... objects) throws SQLException, SQLHandledException {
        change((Object)value, session, objects);
    }
    public void change(LocalDateTime value, ExecutionContext context, DataObject... objects) throws SQLException, SQLHandledException {
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
        setupLoggable(ownerModule, systemEventsLM, null);
        property.setLoggable(true);
    }

    public void makeUserLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM, DBNamingPolicy namingPolicy) {
        setupLoggable(ownerModule, systemEventsLM, namingPolicy);
        property.setLoggable(true);
    }

    private void setupLoggable(LogicsModule ownerModule, SystemEventsLogicsModule systemEventsLM, DBNamingPolicy namingPolicy) {
        if (property.getLogValueProperty() == null) {
            LP logValueProperty = ownerModule.addLProp(systemEventsLM, this, namingPolicy);
            LP logDropProperty = ownerModule.addLDropProp(systemEventsLM, this, namingPolicy);
            
            property.setLogValueProperty(logValueProperty);
            property.setLogWhereProperty(ownerModule.addLWhereProp(logValueProperty, logDropProperty));
        }
        if (property.getLogFormAction() == null) {
            LogFormEntity logFormEntity = new LogFormEntity(null,
                                                            LocalizedString.create("{logics.property.log.form}"),
                                                            this, property.getLogValueProperty(), property.getLogWhereProperty(), systemEventsLM);
            systemEventsLM.addFormEntity(logFormEntity);
            property.setLogFormAction(ownerModule.addMFAProp(LocalizedString.create("{logics.property.log.action}"), logFormEntity, logFormEntity.params, true));
        }
    }

    public <D extends PropertyInterface> void setEventChange(LP<D> valueProperty, int whereNum, Object... params) {

        ImList<PropertyInterfaceImplement<T>> defImplements = readCalcImplements(listInterfaces, params);

        property.setEventChange(LogicsModule.mapCalcListImplement(valueProperty, listInterfaces),
                BaseUtils.immutableCast(defImplements.subList(0, whereNum)),
                BaseUtils.<ImList<PropertyMapImplement<?, T>>>immutableCast(defImplements.subList(whereNum, defImplements.size())).getCol());
    }

    public <D extends PropertyInterface> void setEventChange(LogicsModule lm, boolean action, Object... params) {
        ImList<PropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        property.setEventChange(lm, action, listImplements.get(0), (PropertyMapImplement<PropertyInterface, T>) listImplements.get(1));
    }

    public void addOperand(boolean hasWhen, List<ResolveClassSet> signature, Version version, Object... params) {
        ImList<PropertyInterfaceImplement<T>> readImplements = readCalcImplements(listInterfaces, params);
        PropertyInterfaceImplement<UnionProperty.Interface> operand = (PropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(0);
        if(hasWhen)
            ((CaseUnionProperty)property).addCase((PropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(1), operand, version);
        else {
//            if(((CaseUnionProperty) property).getAbstractType() == CaseUnionProperty.Type.MULTI) {
//                AbstractCase.cntexpl = AbstractCase.cntexpl + 1;
//                if(operand instanceof PropertyMapImplement) {
//                    if(BaseUtils.nullEquals(((PropertyMapImplement)operand).property.getName(), property.getName()))
//                        AbstractCase.cntexplname = AbstractCase.cntexplname + 1;
//                }
//            }
            ((CaseUnionProperty) property).addOperand((PropertyMapImplement<?, UnionProperty.Interface>) operand, signature, version);
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

    public <U> PropertyImplement<T, U> getMapping(U... mapping) {
        return new PropertyImplement<>(property, getMap(mapping));
    }
    public <U extends PropertyInterface> PropertyMapImplement<T, U> getImplement(U... mapping) {
        return new PropertyMapImplement<>(property, getRevMap(mapping));
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

    public LP<T> getOld() {
        return new LP<>(property.getOld(PrevScope.DB), listInterfaces);
    }
    
    public ResolveClassSet getResolveClassSet(List<ResolveClassSet> classes) {
        return property.getResolveClassSet(listInterfaces.mapList(ListFact.fromJavaList(classes)));    
    }
}
