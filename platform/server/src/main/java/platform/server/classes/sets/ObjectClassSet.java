package platform.server.classes.sets;

import platform.server.classes.BaseClass;

public interface ObjectClassSet extends AndClassSet {

    BaseClass getBaseClass();

    public String getWhereString(String source);

    public String getNotWhereString(String source);

}
