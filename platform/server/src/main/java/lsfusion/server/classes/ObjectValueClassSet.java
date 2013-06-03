package lsfusion.server.classes;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.sets.ObjectClassSet;
import lsfusion.server.logics.property.ClassField;

public interface ObjectValueClassSet extends ObjectClassSet, ValueClassSet {

    int getCount();

    int getClassCount();

    ImSet<ConcreteCustomClass> getSetConcreteChildren();

    String getWhereString(String source);

    String getNotWhereString(String source);

    BaseClass getBaseClass();

    ImRevMap<ClassField, ObjectValueClassSet> getTables(); // CustomClass только как хранитель таблицы
}
