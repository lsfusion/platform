package lsfusion.server.data.query.compile;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.reader.Reader;

import java.util.function.Function;

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
        return map.mapOrderValues((CompileOrder value) -> new CompileOrder(value.desc, value.reader, true));
    }

    public static <K> ImOrderMap<K, CompileOrder> reverseOrder(ImOrderMap<K, CompileOrder> map) {
        return map.mapOrderValues((CompileOrder value) -> new CompileOrder(!value.desc, value.reader, value.notNull));
    }

}
