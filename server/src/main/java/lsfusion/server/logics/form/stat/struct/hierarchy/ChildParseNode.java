package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.formula.JSONField;
import lsfusion.server.logics.property.implement.PropertyMapImplement;

public interface ChildParseNode extends ParseNode {

    String getKey();

    default JSONField getField() {
        return new JSONField(getKey());
    }

    default PropertyMapImplement getShowIfProperty(ImRevMap mapObjects) {
        return null;
    }
}
