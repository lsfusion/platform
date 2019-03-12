package lsfusion.server.data;

import lsfusion.base.comb.map.GlobalObject;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;

public interface Value extends ParseValue {

    GlobalObject getValueClass();

    Value removeBig(MAddSet<Value> usedValues);

    String toDebugString();
}
