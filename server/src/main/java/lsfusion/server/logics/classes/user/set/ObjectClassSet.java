package lsfusion.server.logics.classes.user.set;

import lsfusion.server.logics.classes.user.ObjectValueClassSet;

public interface ObjectClassSet extends AndClassSet {

    OrObjectClassSet getOr();

    ObjectValueClassSet getValueClassSet();
}
