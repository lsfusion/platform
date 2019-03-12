package lsfusion.server.data.type;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.base.col.interfaces.mutable.MList;

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
