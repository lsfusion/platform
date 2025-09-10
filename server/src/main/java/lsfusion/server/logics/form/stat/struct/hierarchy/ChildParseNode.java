package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.server.data.expr.formula.FieldShowIf;

public interface ChildParseNode extends ParseNode {

    String getKey();

    default FieldShowIf getFieldShowIf() {
        return null;
    }
}
