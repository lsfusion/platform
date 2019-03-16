package lsfusion.server.logics.classes.user;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.join.classes.IsClassField;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;

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
