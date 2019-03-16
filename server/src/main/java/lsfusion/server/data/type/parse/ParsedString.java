package lsfusion.server.data.type.parse;

import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.mutability.TwinImmutableObject;

public class ParsedString extends TwinImmutableObject {

    protected final String string;

    public ParsedString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return string.equals(((ParsedString)o).string);
    }

    public int immutableHashCode() {
        return string.hashCode();
    }

    public void fillEnv(MList<String> mPreparedParams) {
    }
}
