package lsfusion.gwt.client.navigator.window;

// the show type of a docked open: modal or not, and the window it goes into, always named
public class GDockedShowFormType implements GShowFormType {

    public String window;
    public boolean modal;

    @SuppressWarnings("unused")
    public GDockedShowFormType() {
    }

    public GDockedShowFormType(String window, boolean modal) {
        this.window = window;
        this.modal = modal;
    }

    @Override
    public boolean isDockedModal() {
        return modal;
    }

    @Override
    public boolean isModal() {
        return modal;
    }

    @Override
    public GWindowFormType getWindowType() {
        return new GDockedWindowFormType(window);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GDockedShowFormType && window.equals(((GDockedShowFormType) o).window) && modal == ((GDockedShowFormType) o).modal;
    }

    @Override
    public int hashCode() {
        return window.hashCode() * 31 + (modal ? 1 : 0);
    }
}
