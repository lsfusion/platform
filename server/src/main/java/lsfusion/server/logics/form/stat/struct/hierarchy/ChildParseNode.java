package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.server.data.expr.formula.JSONField;

public interface ChildParseNode extends ParseNode {

    String getKey();

    default JSONField getField() {
        return new JSONField(getKey());
    }
}
