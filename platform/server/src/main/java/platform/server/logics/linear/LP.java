package platform.server.logics.linear;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.PropertyObjectNavigator;
import platform.base.BaseUtils;

import java.util.*;

public class LP<T extends PropertyInterface> extends LC<T,Property<T>> {

    public LP(Property<T> property) {
        super(property);
    }

    public LP(Property<T> property, List<T> listInterfaces) {
        super(property,listInterfaces);
    }

    public <D extends PropertyInterface> void setDerivedChange(LP<D> valueProperty, Object... params) {
        boolean defaultChanged = false;
        if(params[0] instanceof Boolean) {
            defaultChanged = (Boolean)params[0];
            params = Arrays.copyOfRange(params,1,params.length);
        }
        List<PropertyInterfaceImplement<T>> defImplements = BusinessLogics.readImplements(listInterfaces,params);
        new DerivedChange<D,T>(property,BusinessLogics.mapImplement(valueProperty,defImplements.subList(0,valueProperty.listInterfaces.size())),
                BaseUtils.<PropertyInterfaceImplement<T>, PropertyMapImplement<?, T>>immutableCast(defImplements.subList(valueProperty.listInterfaces.size(), defImplements.size())),
                defaultChanged);
    }

    public PropertyObjectNavigator<T> createNavigator(ObjectNavigator... objects) {
        return new PropertyObjectNavigator<T>(this, objects);
    }
}
