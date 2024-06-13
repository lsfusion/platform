package lsfusion.interop.form.property;

public enum EventSource {
    EDIT, BINDING, CUSTOM, PASTE;

    public boolean isExternalChange() {
        return this == CUSTOM || this == PASTE;
    }
}
