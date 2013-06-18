package lsfusion.server.logics.linear;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.LogFormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.*;
import lsfusion.server.logics.property.*;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChange;

import java.sql.SQLException;

import static lsfusion.server.logics.PropertyUtils.mapCalcImplement;
import static lsfusion.server.logics.PropertyUtils.readCalcImplements;

public class LCP<T extends PropertyInterface> extends LP<T, CalcProperty<T>> {

    public LCP(CalcProperty<T> property) {
        super(property);
    }

    public LCP(CalcProperty<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public Object read(FormInstance form, ObjectValue... objects) throws SQLException {
        return property.read(form, getMapValues(objects));
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env, ObjectValue... objects) throws SQLException {
        return property.read(session, getMapValues(objects), modifier, env);
    }

    public Object read(ExecutionContext context, ObjectValue... objects) throws SQLException {
        return read(context.getSession().sql, context.getModifier(), context.getQueryEnv(), objects);
    }

    public Object read(DataSession session, ObjectValue... objects) throws SQLException {
        return read(session.sql, session.getModifier(), session.env, objects);
    }

    public ObjectValue readClasses(DataSession session, Modifier modifier, QueryEnvironment env, ObjectValue... objects) throws SQLException {
        ImMap<T, ObjectValue> mapValues = getMapValues(objects);
        return property.readClasses(session, mapValues, modifier, env);
    }

    public ObjectValue readClasses(ExecutionContext context, DataObject... objects) throws SQLException {
        return readClasses(context.getSession(), context.getModifier(), context.getQueryEnv(), objects);
    }

    public ObjectValue readClasses(DataSession session, DataObject... objects) throws SQLException {
        return readClasses(session, session.getModifier(), session.env, objects);
    }

    // execute'ы без Form'
    public void change(Object value, DataSession session, DataObject... objects) throws SQLException {
        change(value, (ExecutionEnvironment)session, objects);
    }

    public void change(ObjectValue value, ExecutionContext context, DataObject... objects) throws SQLException {
        change(value, context.getEnv(), objects);
    }

    public void change(Object value, ExecutionContext context, DataObject... objects) throws SQLException {
        change(value, context.getEnv(), objects);
    }

    public void change(Object value, ExecutionContext context, ImMap<T, DataObject> keys) throws SQLException {
        change(value, context.getEnv(), keys);
    }

    public void change(ObjectValue value, ExecutionEnvironment env, DataObject... objects) throws SQLException {
        change(value, env, getMapDataValues(objects));
    }

    public void change(Object value, ExecutionEnvironment env, DataObject... objects) throws SQLException {
        change(value, env, getMapDataValues(objects));
    }

    public void change(ObjectValue value, ExecutionEnvironment env, ImMap<T, DataObject> keys) throws SQLException {
        property.change(keys, env, value);
    }

    public void change(Object value, ExecutionEnvironment env, ImMap<T, DataObject> keys) throws SQLException {
        //отдельно обрабатываем false-значения: используем null вместо false
        if (value instanceof Boolean && !(Boolean)value) {
            value = null;
        }
        property.change(keys, env, value);
    }

    public void makeLoggable(SystemEventsLogicsModule systemEventsLM) {
        makeLoggable(systemEventsLM, false);
    }

    public void makeLoggable(SystemEventsLogicsModule systemEventsLM, boolean lazyInit) {
        property.loggable = true;
        setupLoggable(systemEventsLM, lazyInit);
    }

    public void makeUserLoggable(SystemEventsLogicsModule systemEventsLM) {
        setupLoggable(systemEventsLM, false);
    }

    private void setupLoggable(SystemEventsLogicsModule systemEventsLM, boolean lazyInit) {
        if (property.getLogProperty() == null) {
            property.setLogProperty(systemEventsLM.addLProp(systemEventsLM, this));
        }
        if (property.getLogFormProperty() == null) {
            LogFormEntity logFormEntity = new LogFormEntity("log" + BaseUtils.capitalize(property.getSID()) + "Form", ServerResourceBundle.getString("logics.property.log.form"),
                                                            this,
                                                            property.getLogProperty(), systemEventsLM, lazyInit);
            property.setLogFormProperty(systemEventsLM.addMFAProp(ServerResourceBundle.getString("logics.property.log.action"), logFormEntity, logFormEntity.params));
        }
    }

    public <D extends PropertyInterface> void setEventChangePrev(LCP<D> valueProperty, Object... params) {
        setEventChange(false, valueProperty, params);
    }

    public <D extends PropertyInterface> void setEventChangeNew(LCP<D> valueProperty, Object... params) {
        setEventChange(true, valueProperty, params); // assert что все интерфейсы есть
    }

    private <D extends PropertyInterface> void setEventChange(boolean valueChanged, LCP<D> valueProperty, Object... params) {
        setEventChange(valueChanged, valueProperty, 0, params);
    }

    private <D extends PropertyInterface> void setEventChange(boolean valueChanged, LCP<D> valueProperty, int whereNum, Object... params) {
        setEventChange(valueChanged, false, valueProperty, whereNum, params);
    }

    public <D extends PropertyInterface> void setEventChangePrevSet(LCP<D> valueProperty, Object... params) {
        setEventChangeSet(false, valueProperty, params);
    }

    public <D extends PropertyInterface> void setEventChangeNewSet(LCP<D> valueProperty, Object... params) {
        setEventChangeSet(true, valueProperty, params); // params только с интерфейсами
    }

    private <D extends PropertyInterface> void setEventChangeSet(boolean valueChanged, LCP<D> valueProperty, Object... params) {
        setEventChangeSet(valueChanged, 0, valueProperty, params);
    }

    // для DCProp
    public <D extends PropertyInterface> void setEventChange(boolean valueChanged, int whereNum, LCP<D> valueProperty, Object... params) {
        setEventChange(valueChanged, valueProperty, whereNum, params);
    }

    public <D extends PropertyInterface> void setEventChangeSet(boolean valueChanged, int whereNum, LCP<D> valueProperty, Object... params) {
        setEventChange(valueChanged, true, valueProperty, whereNum, params);
    }

    private <D extends PropertyInterface> void setEventChange(boolean valueChanged, boolean setChanged, LCP<D> valueProperty, int whereNum, Object... params) {
        int intValue = valueProperty.listInterfaces.size();
        ImList<CalcPropertyInterfaceImplement<T>> defImplements = readCalcImplements(listInterfaces, params);

        property.setEventChange(valueChanged, setChanged ? IncrementType.SET : IncrementType.SETCHANGED, mapCalcImplement(valueProperty, defImplements.subList(0, intValue)),
                BaseUtils.<ImList<CalcPropertyMapImplement<?, T>>>immutableCast(defImplements.subList(intValue, intValue + whereNum)),
                BaseUtils.<ImList<CalcPropertyMapImplement<?, T>>>immutableCast(defImplements.subList(intValue + whereNum, defImplements.size())).getCol());
    }

    public <D extends PropertyInterface> void setEventChange(Object... params) {
        setEventChange(null, false, params);
    }

    public <D extends PropertyInterface> void setEventChange(LogicsModule lm, boolean action, Object... params) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        property.setEventChange(lm, action, listImplements.get(0), (CalcPropertyMapImplement<PropertyInterface, T>) listImplements.get(1));
    }

    public ImOrderSet<T> listGroupInterfaces;
    public void setDG(boolean ascending, Object... params) {
        setDG(ascending, false, params);
    }
    public void setDG(boolean ascending, boolean over, Object... params) {
        setDG(ascending, over, readCalcImplements(listGroupInterfaces, params));
    }
    public <T extends PropertyInterface> void setDG(boolean ascending, boolean over, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        ((SumGroupProperty<T>)property).setDataChanges(listImplements.subList(1, listImplements.size()).toOrderSet().toOrderMap(ascending),
                (CalcPropertyMapImplement<?, T>) listImplements.get(0), over);
    }

    public void addOperand(boolean hasWhen, Object... params) {
        ImList<CalcPropertyInterfaceImplement<T>> readImplements = readCalcImplements(listInterfaces, params);
        CalcPropertyInterfaceImplement<UnionProperty.Interface> operand = (CalcPropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(0);
        if(hasWhen)
            ((CaseUnionProperty)property).addCase((CalcPropertyInterfaceImplement<UnionProperty.Interface>) readImplements.get(1), operand);
        else
            ((CaseUnionProperty)property).addOperand((CalcPropertyMapImplement<?, UnionProperty.Interface>) operand);
    }

    public ImRevMap<T, KeyExpr> getMapKeys() {
        return property.getMapKeys();
    }

    public Expr getExpr(Modifier modifier, final Expr... exprs) {
        return property.getExpr(getMap(exprs),modifier);
    }

    public Expr getExpr(final Expr... exprs) {
        return property.getExpr(getMap(exprs));
    }

    public <U> CalcPropertyImplement<T, U> getMapping(U... mapping) {
        return new CalcPropertyImplement<T, U>(property, getMap(mapping));
    }
    public <U extends PropertyInterface> CalcPropertyMapImplement<T, U> getImplement(U... mapping) {
        return new CalcPropertyMapImplement<T, U>(property, getRevMap(mapping));
    }

    public PropertyChange<T> getChange(Expr expr, Where where, KeyExpr... keys) {
        return new PropertyChange<T>(getRevMap(keys), expr, where);
    }

    public void setAutoset(boolean autoset) {
        assert property.interfaces.size()==1;
        property.autoset = autoset;
    }

    public ValueClass[] getInterfaceClasses() {
        return property.getInterfaceClasses(listInterfaces);
    }
    
    public LAP getEditAction(String editActionSID) {
        return property.getEditAction(editActionSID).createLP(listInterfaces);
    }

    public <A extends PropertyInterface> void setEditAction(String editActionSID, LAP<A> editAction) {
        property.setEditAction(editActionSID, new ActionPropertyMapImplement<A, T>(editAction.property, editAction.getRevMap(listInterfaces)));
    }
    
    public LCP<T> getOld() {
        return new LCP<T>(property.getOld(PrevScope.DB), listInterfaces);
    }
}
