package lsfusion.interop.form;

// the show type of a docked open: modal or not - a synchronous open blocks the form it was opened from, the way a
// docked-modal form in System.forms does - and the window it goes into
public class DockedShowFormType implements ShowFormType {

    public final String window; // the canonical name of the FORMS window, never null
    public final boolean modal;

    public DockedShowFormType(String window, boolean modal) {
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
    public WindowFormType getWindowType() {
        return new DockedWindowFormType(window);
    }

    @Override
    public String toString() {
        return (modal ? "DOCKED_MODAL " : "DOCKED ") + window;
    }
}
