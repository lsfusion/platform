package platform.server.data.classes.where;

import platform.server.data.classes.BaseClass;

public interface ObjectClassSet extends ClassSet {

    BaseClass getBaseClass();

    public String getWhereString(String source);

    public String getNotWhereString(String source);

}
