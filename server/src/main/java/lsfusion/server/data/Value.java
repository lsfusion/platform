package lsfusion.server.data;

import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.comb.map.GlobalObject;

public interface Value extends ParseValue {

    GlobalObject getValueClass();

    Value removeBig(MAddSet<Value> usedValues);

    String toDebugString();
}
