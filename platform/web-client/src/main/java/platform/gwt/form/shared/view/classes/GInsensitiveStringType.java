package platform.gwt.form.shared.view.classes;

public class GInsensitiveStringType extends GStringType {
    public GInsensitiveStringType() {}

    public GInsensitiveStringType(int length) {
        super(length);
    }

    @Override
    public String toString() {
        return "Строка без регистра(" + length + ")";
    }
}
