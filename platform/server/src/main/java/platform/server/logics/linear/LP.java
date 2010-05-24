package platform.server.logics.linear;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;
import platform.base.BaseUtils;

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

    public <D extends PropertyInterface> void setDerivedChange(LP<D> valueProperty, BusinessLogics<?> BL, Object... params) {
        boolean defaultChanged = false;
        if(params[0] instanceof Boolean) {
            defaultChanged = (Boolean)params[0];
            params = Arrays.copyOfRange(params,1,params.length);
        }
        List<PropertyInterfaceImplement<T>> defImplements = BusinessLogics.readImplements(listInterfaces,params);
        DerivedChange<D,T> derivedChange = new DerivedChange<D,T>(property,BusinessLogics.mapImplement(valueProperty,defImplements.subList(0,valueProperty.listInterfaces.size())),
                BaseUtils.<PropertyInterfaceImplement<T>, PropertyMapImplement<?, T>>immutableCast(defImplements.subList(valueProperty.listInterfaces.size(), defImplements.size())),
                defaultChanged);

        // запишем в DataProperty
        if(BL!=null && derivedChange.notDeterministic())
            BL.notDeterministic.add(derivedChange);
        else
            for(DataProperty dataProperty : property.getDataChanges())
                dataProperty.derivedChange = derivedChange;
    }
}
