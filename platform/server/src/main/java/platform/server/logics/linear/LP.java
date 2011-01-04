package platform.server.logics.linear;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.data.QueryEnvironment;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.*;

public class LP<T extends PropertyInterface> {

    public Property<T> property;
    public List<T> listInterfaces;

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

    public <D extends PropertyInterface> void setDerivedChange(boolean defaultChanged, LP<D> valueProperty, Object... params) {
        setDerivedChange(defaultChanged, valueProperty, null, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(LP<D> valueProperty, BusinessLogics<?> BL, Object... params) {
        if(params[0] instanceof Boolean)
            setDerivedChange((Boolean)params[0], valueProperty, BL, Arrays.copyOfRange(params,1,params.length));
        else
            setDerivedChange(false, valueProperty, BL, params);
    }

    public <D extends PropertyInterface> void setDerivedChange(boolean defaultChanged, LP<D> valueProperty, BusinessLogics<?> BL, Object... params) {
        List<PropertyInterfaceImplement<T>> defImplements = BusinessLogics.readImplements(listInterfaces,params);
        DerivedChange<D,T> derivedChange = new DerivedChange<D,T>(property,BusinessLogics.mapImplement(valueProperty,defImplements.subList(0,valueProperty.listInterfaces.size())),
                BaseUtils.<PropertyInterfaceImplement<T>, PropertyMapImplement<?, T>>immutableCast(defImplements.subList(valueProperty.listInterfaces.size(), defImplements.size())),
                defaultChanged);

        // запишем в DataProperty
        if(BL!=null && derivedChange.notDeterministic())
            BL.notDeterministic.add(derivedChange);
        else
            for(UserProperty dataProperty : property.getDataChanges())
                dataProperty.derivedChange = derivedChange;
    }

    private Map<T, DataObject> getMapValues(DataObject... objects) {
        Map<T, DataObject> mapValues = new HashMap<T, DataObject>();
        for(int i=0;i<listInterfaces.size();i++)
            mapValues.put(listInterfaces.get(i),objects[i]);
        return mapValues;
    }

    public OrderedMap<T, KeyExpr> getMapKeys() {
        return BaseUtils.listMap(listInterfaces, property.getMapKeys());
    }

    public ValueClass[] getMapClasses() {
        return BaseUtils.mapList(listInterfaces, property.getMapClasses()).toArray(new ValueClass[1]);
    }

    public ValueClass[] getCommonClasses(Result<ValueClass> value) {
        Property.CommonClasses<T> common = property.getCommonClasses();
        value.result = common.value;
        return BaseUtils.mapList(listInterfaces, common.interfaces).toArray(new ValueClass[1]);
    }

    public ClassWhere<Integer> getClassWhere() {
        ClassWhere<T> classWhere = property.getClassWhere();
        Map<T, Integer> mapping = new HashMap<T, Integer>();
        for (int i = 0; i < listInterfaces.size(); i++)
            mapping.put(listInterfaces.get(i), i+1);
        return new ClassWhere<Integer>(classWhere, mapping);
    }

    public Expr getExpr(Modifier<? extends Changes> modifier, Expr... exprs) {
        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for(int i=0;i<listInterfaces.size();i++)
            mapExprs.put(listInterfaces.get(i),exprs[i]);        
        return property.getExpr(mapExprs,modifier,null);
    }

    public Expr getExpr(Expr... exprs) {
        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for(int i=0;i<listInterfaces.size();i++)
            mapExprs.put(listInterfaces.get(i),exprs[i]);
        return property.getExpr(mapExprs);
    }

    public Object read(SQLSession session, Modifier<? extends Changes> modifier, QueryEnvironment env, DataObject... objects) throws SQLException {
        Map<T, DataObject> mapValues = getMapValues(objects);
        return property.read(session, mapValues, modifier, env);
    }

    public Object read(DataSession session, DataObject... objects) throws SQLException {
        return read(session.sql, session.modifier, session.env, objects);
    }

    // execute'ы без Form'
    public List<ClientAction> execute(Object value, DataSession session, DataObject... objects) throws SQLException {
        return execute(value, session, session.modifier, objects);
    }

    public List<ClientAction> execute(Object value, DataSession session, Modifier<? extends Changes> modifier, DataObject... objects) throws SQLException {
        Map<T, DataObject> mapKeys = getMapValues(objects);
        return property.execute(mapKeys, session, value, modifier);
    }

    public static List<Property> toPropertyArray(LP[] properties) {
        List<Property> result = new ArrayList<Property>();
        for (LP property : properties)
            result.add(property.property);
        return result;
    }
}
