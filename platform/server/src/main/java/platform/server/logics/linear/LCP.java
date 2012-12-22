package platform.server.logics.linear;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.col.ListFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.ValueClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.entity.LogFormEntity;
import platform.server.form.instance.FormInstance;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.*;
import platform.server.session.DataSession;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.List;

import static platform.server.logics.PropertyUtils.mapCalcImplement;
import static platform.server.logics.PropertyUtils.readCalcImplements;

public class LCP<T extends PropertyInterface> extends LP<T, CalcProperty<T>> {

    public LCP(CalcProperty<T> property) {
        super(property);
    }

    public LCP(CalcProperty<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public void makeUserLoggable(BaseLogicsModule LM) {
        makeUserLoggable(LM, false);
    }

    public Object read(FormInstance form, DataObject... objects) throws SQLException {
        return property.read(form, getMapValues(objects));
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env, DataObject... objects) throws SQLException {
        return property.read(session, getMapValues(objects), modifier, env);
    }

    public Object read(ExecutionContext context, DataObject... objects) throws SQLException {
        return read(context.getSession().sql, context.getModifier(), context.getQueryEnv(), objects);
    }

    public Object read(DataSession session, DataObject... objects) throws SQLException {
        return read(session.sql, session.getModifier(), session.env, objects);
    }

    public ObjectValue readClasses(DataSession session, Modifier modifier, QueryEnvironment env, DataObject... objects) throws SQLException {
        ImMap<T, DataObject> mapValues = getMapValues(objects);
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

    public void change(Object value, ExecutionContext context, DataObject... objects) throws SQLException {
        change(value, context.getEnv(), objects);
    }

    public void change(Object value, ExecutionContext context, ImMap<T, DataObject> keys) throws SQLException {
        change(value, context.getEnv(), keys);
    }

    public void change(Object value, ExecutionEnvironment env, DataObject... objects) throws SQLException {
        change(value, env, getMapValues(objects));
    }

    public void change(Object value, ExecutionEnvironment env, ImMap<T, DataObject> keys) throws SQLException {
        //отдельно обрабатываем false-значения: используем null вместо false
        if (value instanceof Boolean && !(Boolean)value) {
            value = null;
        }
        property.change(keys, env, value);
    }

    public void makeUserLoggable(BaseLogicsModule LM, boolean lazyInit) {
        if (property.getLogProperty() == null) {
            property.setLogProperty(LM.addLProp(this));
        }
        if (property.getLogFormProperty() == null) {
            LogFormEntity logFormEntity = new LogFormEntity("log" + BaseUtils.capitalize(property.getSID()) + "Form", ServerResourceBundle.getString("logics.property.log.form"), (LCP)LM.getLP(property.getSID()), property.getLogProperty(), LM, lazyInit);
            property.setLogFormProperty(LM.addMFAProp(ServerResourceBundle.getString("logics.property.log.action"), logFormEntity, logFormEntity.params));
        }
    }

    public void makeLoggable(BaseLogicsModule LM) {
        makeLoggable(LM, false);
    }

    public void makeLoggable(BaseLogicsModule LM, boolean lazyInit) {
        property.loggable = true;
        makeUserLoggable(LM, lazyInit);
    }

    public void setLogProperty(LCP logProperty) {
        property.setLogProperty(logProperty);
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

        property.setEventChange(valueChanged, setChanged ? IncrementType.SET : IncrementType.LEFTCHANGE, mapCalcImplement(valueProperty, defImplements.subList(0, intValue)),
                BaseUtils.<ImList<CalcPropertyMapImplement<?, T>>>immutableCast(defImplements.subList(intValue, intValue + whereNum)),
                BaseUtils.<ImList<CalcPropertyMapImplement<?, T>>>immutableCast(defImplements.subList(intValue + whereNum, defImplements.size())).getCol());
    }

    public <D extends PropertyInterface> void setEventChange(Object... params) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        property.setEventChange(listImplements.get(0), (CalcPropertyMapImplement<PropertyInterface, T>) listImplements.get(1));
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
        return new LCP<T>(property.getOld(), listInterfaces);
    }
}
