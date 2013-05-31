package platform.server.data.query;

import platform.base.Pair;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.type.Reader;
import platform.server.data.type.Type;

public class CompileOrder {
    public final boolean desc;
    public final Reader reader;
    public final boolean notNull;

    public CompileOrder(boolean desc, Reader reader, boolean notNull) {
        this.desc = desc;
        this.reader = reader;
        this.notNull = notNull;
    }

    public static <K> ImOrderMap<K, CompileOrder> setNotNull(ImOrderMap<K, CompileOrder> map) {
        return map.mapOrderValues(new GetValue<CompileOrder, CompileOrder>() {
            public CompileOrder getMapValue(CompileOrder value) {
                return new CompileOrder(value.desc, value.reader, true);
            }});
    }
}
