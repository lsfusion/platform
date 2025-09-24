package lsfusion.server.data.expr.formula;

import java.util.Objects;

public class JSONField {
    public String name;
    public FieldShowIf showIf;

    public JSONField(String name) {
        this(name, null);
    }

    public JSONField(String name, FieldShowIf showIf) {
        this.name = name;
        this.showIf = showIf;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JSONField jsonField = (JSONField) o;
        return Objects.equals(name, jsonField.name) && showIf == jsonField.showIf;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, showIf);
    }
}
