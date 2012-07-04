package platform.gwt.form.client.events;

import com.google.gwt.event.shared.GwtEvent;
import platform.gwt.view.GForm;

public class OpenFormEvent extends GwtEvent<OpenFormHandler> {
    private static Type<OpenFormHandler> TYPE;

    public static Type<OpenFormHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<OpenFormHandler>();
        }
        return TYPE;
    }


    public static void fireEvent(GForm form) {
        if (TYPE != null) {
            GlobalEventBus.fireEvent(new OpenFormEvent(form));
        }
    }

    public static void fireEvent(String formSID, String caption) {
        if (TYPE != null) {
            GlobalEventBus.fireEvent(new OpenFormEvent(formSID, caption));
        }
    }

    private final String caption;
    private final String formSID;
    private final GForm form;

    public OpenFormEvent(GForm form) {
        this.formSID = null;
        this.caption = null;
        this.form = form;
    }

    public OpenFormEvent(String formSID, String caption) {
        this.formSID = formSID;
        this.caption = caption;
        this.form = null;
    }

    public String getFormSID() {
        return formSID;
    }

    @Override
    public Type<OpenFormHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(OpenFormHandler handler) {
        handler.openForm(this);
    }

    public String getCaption() {
        return caption;
    }
}
