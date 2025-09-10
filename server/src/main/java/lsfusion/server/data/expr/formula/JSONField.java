package lsfusion.server.data.expr.formula;

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
}
