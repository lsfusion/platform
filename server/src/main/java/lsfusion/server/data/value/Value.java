package lsfusion.server.data.value;

import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.server.data.query.compile.ParseValue;

public interface Value extends ParseValue {

    GlobalObject getValueClass();

    Value removeBig(MAddSet<Value> usedValues);

    String toDebugString();
}
