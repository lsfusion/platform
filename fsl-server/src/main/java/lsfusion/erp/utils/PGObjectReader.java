package lsfusion.erp.utils;

import lsfusion.base.ExtInt;
import lsfusion.server.data.type.AbstractReader;
import org.postgresql.util.PGobject;

public class PGObjectReader<T> extends AbstractReader<String> {

    public static final PGObjectReader instance = new PGObjectReader();

    @Override
    public String read(Object value) {
        if (value == null) return null;
        if (value instanceof PGobject) {
            return ((PGobject) value).getValue();
        } else throw new UnsupportedOperationException();
    }

    @Override
    public ExtInt getCharLength() {
        return new ExtInt(100);
    }
}
