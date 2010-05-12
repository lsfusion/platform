package platform.server.view.form;

import platform.server.classes.sets.AndClassSet;
import platform.server.classes.CustomClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class PropertyObjectImplement<P extends PropertyInterface> extends ControlObjectImplement<P,Property<P>> implements OrderView {

    public PropertyObjectImplement(Property<P> property,Map<P,? extends PropertyObjectInterface> mapping) {
        super(property, mapping);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return changedProps.contains(property);
    }

    public void fillProperties(Set<Property> properties) {
        properties.add(property);
    }

    public Map<P, DataObject> getInterfaceValues() {
        Map<P,DataObject> mapInterface = new HashMap<P,DataObject>();
        for(Map.Entry<P,PropertyObjectInterface> implement : mapping.entrySet())
            mapInterface.put(implement.getKey(),implement.getValue().getDataObject());
        return mapInterface;
    }

    public PropertyValueImplement getChangeProperty() {
        return property.getChangeProperty(getInterfaceValues());
    }

    public Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource, Modifier<? extends Changes> modifier) throws SQLException {

        Map<P, Expr> joinImplement = new HashMap<P, Expr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getExpr(classGroup, classSource, modifier));
        return property.getExpr(joinImplement,modifier,null);
    }

    public Type getType() {
        return property.getType();
    }

    public CustomClass getDialogClass() {
        Map<P, ConcreteClass> mapClasses = new HashMap<P, ConcreteClass>();
        for(Map.Entry<P,PropertyObjectInterface> implement : mapping.entrySet())
            mapClasses.put(implement.getKey(),implement.getValue().getCurrentClass());
        return property.getDialogClass(getInterfaceValues(), mapClasses);
    }
}
