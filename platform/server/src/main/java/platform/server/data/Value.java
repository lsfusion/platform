package platform.server.data;

import platform.base.GlobalObject;
import platform.base.col.interfaces.mutable.add.MAddSet;
import platform.server.data.type.ParseInterface;

public interface Value extends ParseValue {

    GlobalObject getValueClass();

    public Value removeBig(MAddSet<Value> usedValues);
}
