package platform.server.logics.linear;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.action.ClientAction;
import platform.server.classes.ActionClass;
import platform.server.classes.ValueClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.*;

import javax.swing.*;
import java.sql.SQLException;
import java.util.*;

import static platform.server.logics.PropertyUtils.mapCalcImplement;
import static platform.server.logics.PropertyUtils.readCalcImplements;

public abstract class LP<T extends PropertyInterface, P extends Property<T>> {

    public P property;
    public List<T> listInterfaces;
    private String creationScript = null;

    public <IT extends PropertyInterface> boolean intersect(LP<IT, ?> lp) {
        assert listInterfaces.size()==lp.listInterfaces.size();
        Map<IT,T> map = new HashMap<IT,T>();
        for(int i=0;i<listInterfaces.size();i++)
            map.put(lp.listInterfaces.get(i),listInterfaces.get(i));
        return property.intersect(lp.property,map);
    }

    public LP(P property) {
        this.property = property;
        listInterfaces = new ArrayList<T>(property.interfaces);
    }

    public LP(P property, List<T> listInterfaces) {
        this.property = property;
        this.listInterfaces = listInterfaces;
    }

    public <D extends PropertyInterface> void setEvent(LP<D> valueProperty, Object... params) {
        setEvent(false, valueProperty, params);
    }
    
    public <D extends PropertyInterface> void setEventChanged(LP<D> valueProperty, Object... params) {
        setEvent(true, valueProperty, params); // assert что все интерфейсы есть
    }

    private <D extends PropertyInterface> void setEvent(boolean valueChanged, LP<D> valueProperty, Object... params) {
        setEvent(valueChanged, valueProperty, 0, params);
    }

    private <D extends PropertyInterface> void setEvent(boolean valueChanged, LP<D> valueProperty, int whereNum, Object... params) {
        setEvent(valueChanged, false, valueProperty, whereNum, params);
    }

    public void setEventAction(Object... params) {
        assert property instanceof ActionProperty;
        setEvent(new LP(DerivedProperty.createStatic(true, ActionClass.instance).property), params);
    }

    public void setEventSetAction(Object... params) {
        assert property instanceof ActionProperty; // там все еще местами Join на Action используется
        setEventSet(new LP(DerivedProperty.createStatic(true, ActionClass.instance).property), params);
    }

    public <D extends PropertyInterface> void setEventSet(LP<D> valueProperty, Object... params) {
        setEventSet(false, valueProperty, params);
    }

    public <D extends PropertyInterface> void setEventChangedSet(LP<D> valueProperty, Object... params) {
        setEventSet(true, valueProperty, params); // params только с интерфейсами
    }

    private <D extends PropertyInterface> void setEventSet(boolean valueChanged, LP<D> valueProperty, Object... params) {
        setEventSet(valueChanged, 0, valueProperty, params);
    }

    // для DCProp
    public <D extends PropertyInterface> void setEvent(boolean valueChanged, int whereNum, LP<D> valueProperty, Object... params) {
        setEvent(valueChanged, valueProperty, whereNum, params);
    }

    public <D extends PropertyInterface> void setEventSet(boolean valueChanged, int whereNum, LP<D> valueProperty, Object... params) {
        setEvent(valueChanged, true, valueProperty, whereNum, params);
    }

    // для DSL
    public <D extends PropertyInterface> void setEvent(boolean valueChanged, boolean forceChanged, LP<D> valueProperty, Object... params) {
        setEvent(valueChanged, forceChanged, valueProperty, 0, params);
    }
    private <D extends PropertyInterface> void setEvent(boolean valueChanged, boolean setChanged, LP<D> valueProperty, int whereNum, Object... params) {
        int intValue = valueProperty.listInterfaces.size();
        List<CalcPropertyInterfaceImplement<T>> defImplements = readCalcImplements(listInterfaces, params);

        property.setEvent(valueChanged, setChanged ? IncrementType.SET : IncrementType.LEFTCHANGE, mapCalcImplement(valueProperty, defImplements.subList(0, intValue)),
                BaseUtils.<CalcPropertyInterfaceImplement<T>, CalcPropertyMapImplement<?, T>>immutableCast(defImplements.subList(intValue, intValue + whereNum)),
                BaseUtils.<CalcPropertyInterfaceImplement<T>, CalcPropertyMapImplement<?, T>>immutableCast(defImplements.subList(intValue + whereNum, defImplements.size())));
    }

    public <D extends PropertyInterface> void setEvent(Object... params) {
        List<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        property.setEvent(listImplements.get(0), (CalcPropertyMapImplement<PropertyInterface, T>) listImplements.get(1));
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

    public Map<T, DataObject> getMapValues(DataObject... objects) {
        Map<T, DataObject> mapValues = new HashMap<T, DataObject>();
        for(int i=0;i<listInterfaces.size();i++)
            mapValues.put(listInterfaces.get(i),objects[i]);
        return mapValues;
    }

    public OrderedMap<T, KeyExpr> getMapKeys() {
        return BaseUtils.orderMap(property.getMapKeys(), listInterfaces);
    }

    public ValueClass[] getMapClasses() {
        return BaseUtils.mapList(listInterfaces, property.getMapClasses()).toArray(new ValueClass[0]);
    }

    public ValueClass[] getCommonClasses(Result<ValueClass> value) {
        Property.CommonClasses<T> common = property.getCommonClasses();
        value.result = common.value;
        return BaseUtils.mapList(listInterfaces, common.interfaces).toArray(new ValueClass[0]);
    }

    public ValueClass getResultClass() {
        Result<ValueClass> result = new Result<ValueClass>();
        getCommonClasses(result);
        return result.result;
    }

    public ClassWhere<Integer> getClassWhere() {
        ClassWhere<T> classWhere = property.getClassWhere();
        Map<T, Integer> mapping = new HashMap<T, Integer>();
        for (int i = 0; i < listInterfaces.size(); i++)
            mapping.put(listInterfaces.get(i), i+1);
        return new ClassWhere<Integer>(classWhere, mapping);
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

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env, DataObject... objects) throws SQLException {
        Map<T, DataObject> mapValues = getMapValues(objects);
        return ((CalcProperty<T>)property).read(session, mapValues, modifier, env);
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
        return ((CalcProperty<T>)property).readClasses(session, mapValues, modifier, env);
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
        assert property instanceof CalcProperty;
        return change(value, new ExecutionEnvironment(session), objects);
    }

    public List<ClientAction> change(Object value, ExecutionContext context, DataObject... objects) throws SQLException {
        assert property instanceof CalcProperty;
        return change(value, context.getEnv(), objects);
    }

    public List<ClientAction> change(Object value, ExecutionContext context, Map<T, DataObject> keys) throws SQLException {
        assert property instanceof CalcProperty;
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
        return ((CalcProperty)property).change(keys, env, value);
    }

    public List<ClientAction> execute(DataSession session, DataObject... objects) throws SQLException {
        return ((ActionProperty)property).execute(ActionProperty.cast(getMapValues(objects)), new ExecutionEnvironment(session), null);
    }

    public static List<Property> toPropertyArray(LP[] properties) {
        List<Property> result = new ArrayList<Property>();
        for (LP property : properties)
            result.add(property.property);
        return result;
    }

    public void setMinimumWidth(int charWidth) {
        property.minimumCharWidth = charWidth;
    }

    public void setPreferredWidth(int charWidth) {
        property.preferredCharWidth = charWidth;
    }

    public void setMaximumWidth(int charWidth) {
        property.maximumCharWidth = charWidth;
    }

    public <U> CalcPropertyImplement<T, U> getMapping(U... mapping) {
        return new CalcPropertyImplement<T, U>((CalcProperty<T>)property, getMap(mapping));
    }

    public <U> Map<T, U> getMap(U... mapping) {
        Map<T,U> propertyMapping = new HashMap<T, U>();
        for(int i=0;i<listInterfaces.size();i++)
            propertyMapping.put(listInterfaces.get(i), mapping[i]);
        return propertyMapping;
    }

    public PropertyChange<T> getChange(Expr expr, Where where, KeyExpr... keys) {
        Map<T, KeyExpr> mapKeys = new HashMap<T, KeyExpr>();
        for(int i=0;i<listInterfaces.size();i++)
            mapKeys.put(listInterfaces.get(i), keys[i]);
        return new PropertyChange<T>(mapKeys, expr, where);
    }
    /*
    public <L extends PropertyInterface> void follows(LP<L> lp, int... mapping) {
        Map<L, T> mapInterfaces = new HashMap<L, T>();
        for(int i=0;i<lp.listInterfaces.size();i++)
            mapInterfaces.put(lp.listInterfaces.get(i), listInterfaces.get(mapping[i]-1));
        property.addFollows(new CalcPropertyMapImplement<L, T>(lp.property, mapInterfaces));
    }

    public void followed(LP... lps) {
        int[] mapping = new int[listInterfaces.size()];
        for(int i=0;i<mapping.length;i++)
            mapping[i] = i+1;
        for(LP lp : lps)
            lp.follows(this, mapping);
    }
    */
    
    public void setMinimumCharWidth(int charWidth) {
        property.minimumCharWidth = charWidth;
    }

    public void setPreferredCharWidth(int charWidth) {
        property.preferredCharWidth = charWidth;
    }

    public void setMaximumCharWidth(int charWidth) {
        property.maximumCharWidth = charWidth;
    }

    public void setFixedCharWidth(int charWidth) {
        property.setFixedCharWidth(charWidth);
    }

    public void setLoggable(boolean loggable) {
        property.loggable = loggable;
    }

    public void setLogProperty(LP logProperty) {
        property.setLogProperty(logProperty);
    }

    public void setLogFormProperty(LP logFormPropertyProp) {
        property.setLogFormProperty(logFormPropertyProp);
    }

     public void makeUserLoggable(BaseLogicsModule LM) {
         makeUserLoggable(LM, false);
    }

    public void makeUserLoggable(BaseLogicsModule LM, boolean lazyInit) {
        if (property.getLogProperty() == null) {
            property.setLogProperty(LM.addLProp(this));
        }
        if (property.getLogFormProperty() == null) {
            LogFormEntity logFormEntity = new LogFormEntity("log" + BaseUtils.capitalize(property.getSID()) + "Form", ServerResourceBundle.getString("logics.property.log.form"), LM.getLP(property.getSID()), property.getLogProperty(), LM, lazyInit);
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

    public void setImage(String name) {
        property.setImage(name);
    }

    public void setEditKey(KeyStroke editKey) {
        property.editKey = editKey;
    }

    public void setShowEditKey(boolean showEditKey) {
        property.showEditKey = showEditKey;
    }

    public void setRegexp(String regexp) {
        property.regexp = regexp;
    }

    public void setRegexpMessage(String regexpMessage) {
        property.regexpMessage = regexpMessage;
    }

    public void setEchoSymbols(boolean echoSymbols) {
        property.echoSymbols = echoSymbols;
    }

    public void setPanelLocation(PanelLocation panelLocation) {
        property.panelLocation = panelLocation;
    }

    public void setShouldBeLast(boolean shouldBeLast) {
        property.shouldBeLast = shouldBeLast;
    }

    public void setForceViewType(ClassViewType forceViewType) {
        property.forceViewType = forceViewType;
    }

    public void setAutoset(boolean autoset) {
        assert property.interfaces.size()==1;
        property.autoset = autoset;
    }

    public void setAskConfirm(boolean askConfirm) {
        property.askConfirm = askConfirm;
    }

    public String getCreationScript() {
        return creationScript;
    }

    public void setCreationScript(String creationScript) {
        this.creationScript = creationScript;
    }
    
    public PropertyObjectEntity<T, ?> createObjectEntity(PropertyObjectInterfaceEntity... objects) {
        return PropertyObjectEntity.create(property, getMap(objects), creationScript);
    }

}
