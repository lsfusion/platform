package platform.server.logics.classes.sets;

import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.properties.PropertyInterface;

public interface PropertyClass<P extends PropertyInterface> {
    // каких классов могут быть объекты при таких классах на входе !!!! Можно дать больше но никак не меньше
    ClassSet getValueClass(InterfaceClass<P> interfaceImplement);
    // при каких классах мы можем получить хоть один такой класс (при других точно не можем) !!! опять таки лучше больше чем меньше
    InterfaceClassSet<P> getClassSet(ClassSet reqValue);

    // на самом деле нам нужен iterator по <InterfaceClass,ClassSet>
    ValueClassSet<P> getValueClassSet();
}
