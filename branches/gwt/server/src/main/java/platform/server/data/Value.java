package platform.server.data;

import platform.server.data.type.TypeObject;
import platform.server.data.type.ParseInterface;
import platform.base.GlobalObject;

public interface Value {

    ParseInterface getParseInterface();
    GlobalObject getValueClass();

}
