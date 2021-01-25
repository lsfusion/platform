package lsfusion.gwt.client.form.property.async;

public class GAsyncOpenForm extends GAsyncExec {
    public String caption;
    public boolean modal;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncOpenForm() {
    }

    public GAsyncOpenForm(String caption, boolean modal) {
        this.caption = caption;
        this.modal = modal;
    }
}