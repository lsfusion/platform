package platform.server.data;

import platform.base.QuickSet;
import platform.server.data.type.TypeObject;
import platform.server.data.type.ParseInterface;
import platform.base.GlobalObject;

import java.util.Set;

public interface Value {

    ParseInterface getParseInterface();
    GlobalObject getValueClass();

    public Value removeBig(QuickSet<Value> usedValues);
}
