package platform.server.logics.linear;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.entity.LogFormEntity;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.server.logics.PropertyUtils.mapCalcImplement;
import static platform.server.logics.PropertyUtils.readCalcImplements;

public class LCP<T extends PropertyInterface> extends LP<T, CalcProperty<T>> {

    public LCP(CalcProperty<T> property) {
        super(property);
    }

    public LCP(CalcProperty<T> property, List<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public static List<CalcProperty> toPropertyArray(LCP[] properties) {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (LCP<?> property : properties)
            result.add(property.property);
        return result;
    }

    public void makeUserLoggable(BaseLogicsModule LM) {
        makeUserLoggable(LM, false);
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env, DataObject... objects) throws SQLException {
        Map<T, DataObject> mapValues = getMapValues(objects);
        return property.read(session, mapValues, modifier, env);
    }

    public Object read(ExecutionContext context, DataObject... objects) throws SQLException {
        return read(context.getSession(), context.getModifier(), objects);
    }

    public Object read(DataSession session, Modifier modifier, DataObject... objects) throws SQLException {
        return read(session.sql, modifier, session.env, objects);
    }

    public Object read(DataSession session, DataObject... objects) throws SQLException {
        return read(session.sql, session.modifier, session.env, objects);
    }

    public ObjectValue readClasses(DataSession session, Modifier modifier, QueryEnvironment env, DataObject... objects) throws SQLException {
        Map<T, DataObject> mapValues = getMapValues(objects);
        return property.readClasses(session, mapValues, modifier, env);
    }

    public ObjectValue readClasses(ExecutionContext context, DataObject... objects) throws SQLException {
        return readClasses(context.getSession(), context.getModifier(), objects);
    }

    public ObjectValue readClasses(DataSession session, Modifier modifier, DataObject... objects) throws SQLException {
        return readClasses(session, modifier, session.env, objects);
    }

    public ObjectValue readClasses(DataSession session, DataObject... objects) throws SQLException {
        return readClasses(session, session.modifier, session.env, objects);
    }

    // execute'ы без Form'
    public List<ClientAction> change(Object value, DataSession session, DataObject... objects) throws SQLException {
        return change(value, new ExecutionEnvironment(session), objects);
    }

    public List<ClientAction> change(Object value, ExecutionContext context, DataObject... objects) throws SQLException {
        return change(value, context.getEnv(), objects);
    }

    public List<ClientAction> change(Object value, ExecutionContext context, Map<T, DataObject> keys) throws SQLException {
        return change(value, context.getEnv(), keys);
    }

    public List<ClientAction> change(Object value, ExecutionEnvironment env, DataObject... objects) throws SQLException {
        return change(value, env, getMapValues(objects));
    }

    public List<ClientAction> change(Object value, ExecutionEnvironment env, Map<T, DataObject> keys) throws SQLException {
        //отдельно обрабатываем false-значения: используем null вместо false
        if (value instanceof Boolean && !(Boolean)value) {
            value = null;
        }
        return property.change(keys, env, value);
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
        List<CalcPropertyInterfaceImplement<T>> defImplements = readCalcImplements(listInterfaces, params);

        property.setEventChange(valueChanged, setChanged ? IncrementType.SET : IncrementType.LEFTCHANGE, mapCalcImplement(valueProperty, defImplements.subList(0, intValue)),
                BaseUtils.<CalcPropertyInterfaceImplement<T>, CalcPropertyMapImplement<?, T>>immutableCast(defImplements.subList(intValue, intValue + whereNum)),
                BaseUtils.<CalcPropertyInterfaceImplement<T>, CalcPropertyMapImplement<?, T>>immutableCast(defImplements.subList(intValue + whereNum, defImplements.size())));
    }

    public <D extends PropertyInterface> void setEventChange(Object... params) {
        List<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        property.setEventChange(listImplements.get(0), (CalcPropertyMapImplement<PropertyInterface, T>) listImplements.get(1));
    }

    public List<T> listGroupInterfaces;
    public void setDG(boolean ascending, Object... params) {
        setDG(ascending, false, params);
    }
    public void setDG(boolean ascending, boolean over, Object... params) {
        setDG(ascending, over, readCalcImplements(listGroupInterfaces, params));
    }
    public <T extends PropertyInterface> void setDG(boolean ascending, boolean over, List<CalcPropertyInterfaceImplement<T>> listImplements) {
        ((SumGroupProperty<T>)property).setDataChanges(new OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean>(listImplements.subList(1, listImplements.size()), ascending),
                (CalcPropertyMapImplement<?, T>) listImplements.get(0), over);
    }

    public void addOperand(Object... params) {
        CalcPropertyMapImplement<?, UnionProperty.Interface> operand = (CalcPropertyMapImplement<?, UnionProperty.Interface>) readCalcImplements(listInterfaces, params).get(0);
        ((ExclusiveUnionProperty)property).addOperand(operand);
    }

    public OrderedMap<T, KeyExpr> getMapKeys() {
        return BaseUtils.orderMap(property.getMapKeys(), listInterfaces);
    }

    public ValueClass[] getCommonClasses(Result<ValueClass> value) {
        value.result = property.getValueClass();
        return BaseUtils.mapList(listInterfaces, property.getMapClasses()).toArray(new ValueClass[0]);
    }

    public ValueClass getResultClass() {
        Result<ValueClass> result = new Result<ValueClass>();
        getCommonClasses(result);
        return result.result;
    }

    public Expr getExpr(Modifier modifier, Expr... exprs) {
        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for(int i=0;i<listInterfaces.size();i++)
            mapExprs.put(listInterfaces.get(i),exprs[i]);
        return property.getExpr(mapExprs,modifier);
    }

    public Expr getExpr(Expr... exprs) {
        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for(int i=0;i<listInterfaces.size();i++)
            mapExprs.put(listInterfaces.get(i),exprs[i]);
        return property.getExpr(mapExprs);
    }

    public <U> CalcPropertyImplement<T, U> getMapping(U... mapping) {
        return new CalcPropertyImplement<T, U>(property, getMap(mapping));
    }

    public PropertyChange<T> getChange(Expr expr, Where where, KeyExpr... keys) {
        Map<T, KeyExpr> mapKeys = new HashMap<T, KeyExpr>();
        for(int i=0;i<listInterfaces.size();i++)
            mapKeys.put(listInterfaces.get(i), keys[i]);
        return new PropertyChange<T>(mapKeys, expr, where);
    }

    public void setAutoset(boolean autoset) {
        assert property.interfaces.size()==1;
        property.autoset = autoset;
    }

    public ValueClass[] getMapClasses() {
        return BaseUtils.mapList(listInterfaces, property.getMapClasses()).toArray(new ValueClass[0]);
    }
}
