package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.Pair;

public interface ChildParseNode extends ParseNode {

    String getKey();

    default Pair<Boolean, Boolean> getOptions() {
        return Pair.create(false, true);
    }
}
