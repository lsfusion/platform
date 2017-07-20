package lsfusion.server.classes;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.sets.ObjectClassSet;
import lsfusion.server.logics.property.IsClassField;
import lsfusion.server.logics.property.ObjectClassField;

public interface ObjectValueClassSet extends ObjectClassSet, ValueClassSet {

    int getCount();

    int getClassCount();

    ImSet<ConcreteCustomClass> getSetConcreteChildren();

    String getWhereString(String source);

    String getNotWhereString(String source);

    BaseClass getBaseClass();

    boolean hasComplex();

    ImRevMap<ObjectClassField, ObjectValueClassSet> getObjectClassFields(); // CustomClass только как хранитель таблицы

    ImRevMap<IsClassField, ObjectValueClassSet> getIsClassFields(); // CustomClass только как хранитель таблицы

    ImRevMap<IsClassField, ObjectValueClassSet> getClassFields(boolean onlyObjectClassFields); // по сути protected

}
