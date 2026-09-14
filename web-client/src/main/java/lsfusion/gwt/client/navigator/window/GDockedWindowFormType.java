package lsfusion.gwt.client.navigator.window;

// DOCKED: the window the form is docked into, always named - System.forms when DOCKED named none. Compared by
// value: an async placeholder and the form that arrives for it each carry their own copy
public class GDockedWindowFormType implements GWindowFormType {

    public String window; // the canonical name of the FORMS window, never null

    @SuppressWarnings("unused")
    public GDockedWindowFormType() {
    }

    public GDockedWindowFormType(String window) {
        this.window = window;
    }

    @Override
    public boolean isDocked() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GDockedWindowFormType && window.equals(((GDockedWindowFormType) o).window);
    }

    @Override
    public int hashCode() {
        return window.hashCode();
    }
}
