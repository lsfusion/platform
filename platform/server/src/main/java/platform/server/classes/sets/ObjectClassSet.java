package platform.server.classes.sets;

import platform.server.classes.BaseClass;

public interface ObjectClassSet extends AndClassSet {

    BaseClass getBaseClass();

    String getWhereString(String source);

    String getNotWhereString(String source);

    OrObjectClassSet getOr();
}
