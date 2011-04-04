package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.classes.CustomClass;
import platform.server.logics.property.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class AggregateGroupProperty extends CycleGroupProperty<ClassPropertyInterface, PropertyInterface> {

    private static <T extends PropertyInterface> PropertyMapImplement<T, ClassPropertyInterface> getSingleImplement(Property<T> property, ObjectValueProperty objectProperty) {
        return new PropertyMapImplement<T, ClassPropertyInterface>(property, Collections.singletonMap(BaseUtils.single(property.interfaces), BaseUtils.single(objectProperty.interfaces)));
    }

    private static Collection<PropertyInterfaceImplement<ClassPropertyInterface>> initInterfaces(ObjectValueProperty objectProperty, Collection<Property> interfaces) {
        Collection<PropertyInterfaceImplement<ClassPropertyInterface>> result = new ArrayList<PropertyInterfaceImplement<ClassPropertyInterface>>();
        for(Property<?> property : interfaces)
            result.add(getSingleImplement(property, objectProperty));
        return result;
    }

    public AggregateGroupProperty(String sID, String caption, ObjectValueProperty objectProperty, Collection<PropertyInterfaceImplement<ClassPropertyInterface>> interfaces) {
        super(sID, caption, interfaces, objectProperty, null);
    }
}



