package lsfusion.server.logics.classes.sets;

import lsfusion.server.logics.classes.ObjectValueClassSet;

public interface ObjectClassSet extends AndClassSet {

    OrObjectClassSet getOr();

    ObjectValueClassSet getValueClassSet();
}
