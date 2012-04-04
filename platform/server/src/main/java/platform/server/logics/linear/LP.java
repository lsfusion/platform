package platform.server.logics.linear;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.LogFormEntity;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.*;
import platform.server.logics.property.*;
import platform.server.session.*;

import javax.swing.*;
import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.immutableCast;
import static platform.server.logics.PropertyUtils.mapImplement;
import static platform.server.logics.PropertyUtils.readImplements;

public class LP<T extends PropertyInterface> {

    public Property<T> property;
    public List<T> listInterfaces;
    private String creationScript = null;

    public <IT extends PropertyInterface> boolean intersect(LP<IT> lp) {
        assert listInterfaces.size()==lp.listInterfaces.size();
        Map<IT,T> map = new HashMap<IT,T>();
        for(int i=0;i<listInterfaces.size();i++)
            map.put(lp.listInterfaces.get(i),listInterfaces.get(i));
        return property.intersect(lp.property,map);
    }

    public LP(Property<T> property) {
        this.property = property;
        listInterfaces = new ArrayList<T>(property.interfaces);
    }

    public LP(Property<T> property, List<T> listInterfaces) {
        this.property = property;
        this.listInterfaces = listInterfaces;
    }

    public <D extends PropertyInterface> void setDerivedChange(LP<D> valueProperty, Object... params) {
        setDerivedChange(valueProperty, null, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(boolean valueChanged, LP<D> valueProperty, Object... params) {
        setDerivedChange(valueChanged, valueProperty, null, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(boolean valueChanged, int whereNum, LP<D> valueProperty, Object... params) {
        setDerivedChange(valueChanged, valueProperty, whereNum, null, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(LP<D> valueProperty, BusinessLogics<?> BL, Object... params) {
        if(params[0] instanceof Boolean)
            setDerivedChange((Boolean)params[0], valueProperty, BL, Arrays.copyOfRange(params,1,params.length));
        else
            setDerivedChange(false, valueProperty, BL, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(boolean valueChanged, LP<D> valueProperty, BusinessLogics<?> BL, Object... params) {
        setDerivedChange(valueChanged, valueProperty, 0, BL, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(boolean valueChanged, LP<D> valueProperty, int whereNum, BusinessLogics<?> BL, Object... params) {
        setDerivedChange(valueChanged, false, valueProperty, whereNum, params);
    }

    public <D extends PropertyInterface> void setDerivedForcedChange(LP<D> valueProperty, Object... params) {
        setDerivedForcedChange(false, valueProperty, params);
    }

    public <D extends PropertyInterface> void setDerivedForcedChange(boolean valueChanged, LP<D> valueProperty, Object... params) {
        setDerivedForcedChange(valueChanged, 0, valueProperty, params);
    }

    public <D extends PropertyInterface> void setDerivedForcedChange(boolean valueChanged, int whereNum, LP<D> valueProperty, Object... params) {
        setDerivedChange(valueChanged, true, valueProperty, whereNum, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(boolean valueChanged, boolean forceChanged, LP<D> valueProperty, Object... params) {
        setDerivedChange(valueChanged, forceChanged, valueProperty, 0, params);
    }
    public <D extends PropertyInterface> void setDerivedChange(boolean valueChanged, boolean setChanged, LP<D> valueProperty, int whereNum, Object... params) {
        int intValue = valueProperty.listInterfaces.size();
        List<PropertyInterfaceImplement<T>> defImplements = readImplements(listInterfaces, params);

        property.setDerivedChange(valueChanged, setChanged ? IncrementType.SET : IncrementType.LEFTCHANGE, mapImplement(valueProperty, defImplements.subList(0, intValue)),
                BaseUtils.<PropertyInterfaceImplement<T>, PropertyMapImplement<?, T>>immutableCast(defImplements.subList(intValue, intValue + whereNum)),
                BaseUtils.<PropertyInterfaceImplement<T>, PropertyMapImplement<?, T>>immutableCast(defImplements.subList(intValue + whereNum, defImplements.size())));
    }

    public List<T> listGroupInterfaces;
    public void setDG(boolean ascending, Object... params) {
        setDG(ascending, false, params);
    }
    public void setDG(boolean ascending, boolean over, Object... params) {
        setDG(ascending, over, readImplements(listGroupInterfaces, params));
    }
    public <T extends PropertyInterface> void setDG(boolean ascending, boolean over, List<PropertyInterfaceImplement<T>> listImplements) {
        ((SumGroupProperty<T>)property).setDataChanges(new OrderedMap<PropertyInterfaceImplement<T>, Boolean>(listImplements.subList(1, listImplements.size()), ascending),
                (PropertyMapImplement<?, T>) listImplements.get(0), over);

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
    public List<ClientAction> execute(Object value, DataSession session, DataObject... objects) throws SQLException {
        return execute(value, session, session.modifier, objects);
    }

    public List<ClientAction> execute(Object value, ExecutionContext context, DataObject... objects) throws SQLException {
        return execute(value, context.getSession(), context.getModifier(), objects);
    }

    public List<ClientAction> execute(Object value, ExecutionContext context, Map<T, DataObject> keys) throws SQLException {
        return execute(value, context.getSession(), context.getModifier(), keys);
    }

    public List<ClientAction> execute(Object value, DataSession session, Modifier modifier, DataObject... objects) throws SQLException {
        return execute(value, session, modifier, getMapValues(objects));
    }

    public List<ClientAction> execute(Object value, DataSession session, Modifier modifier, Map<T, DataObject> keys) throws SQLException {
        //отдельно обрабатываем false-значения: используем null вместо false
        if (value instanceof Boolean && !(Boolean)value) {
            value = null;
        }
        return property.execute(keys, session, value, modifier);
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

    public <U> PropertyImplement<T, U> getMapping(U... mapping) {
        Map<T,U> propertyMapping = new HashMap<T, U>();
        for(int i=0;i<listInterfaces.size();i++)
            propertyMapping.put(listInterfaces.get(i), mapping[i]);
        return new PropertyImplement<T, U>(property, propertyMapping);
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
        property.addFollows(new PropertyMapImplement<L, T>(lp.property, mapInterfaces));
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

    public LP setImage(String name) {
        property.setImage(name);
        return this;
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
}
