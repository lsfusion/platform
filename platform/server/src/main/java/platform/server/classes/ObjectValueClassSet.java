package platform.server.classes;

import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.logics.property.ClassField;

public interface ObjectValueClassSet extends ObjectClassSet, ValueClassSet {

    int getCount();

    int getClassCount();

    ImSet<ConcreteCustomClass> getSetConcreteChildren();

    String getWhereString(String source);

    String getNotWhereString(String source);

    BaseClass getBaseClass();

    ImRevMap<ClassField, ObjectValueClassSet> getTables(); // CustomClass только как хранитель таблицы
}
