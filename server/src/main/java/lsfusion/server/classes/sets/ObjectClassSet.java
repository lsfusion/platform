package lsfusion.server.classes.sets;

import lsfusion.server.classes.ObjectValueClassSet;

public interface ObjectClassSet extends AndClassSet {

    OrObjectClassSet getOr();

    ObjectValueClassSet getValueClassSet();
}
