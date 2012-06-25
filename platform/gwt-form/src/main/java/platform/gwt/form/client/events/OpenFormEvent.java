package platform.gwt.form.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class OpenFormEvent extends GwtEvent<OpenFormHandler> {
    private static Type<OpenFormHandler> TYPE;

    public static Type<OpenFormHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<OpenFormHandler>();
        }
        return TYPE;
    }


    public static void fireEvent(String formSID, String caption) {
        if (TYPE != null) {
            GlobalEventBus.fireEvent(new OpenFormEvent(formSID, caption));
        }
    }

    private final String caption;
    private final String formSID;

    public OpenFormEvent(String formSID, String caption) {
        this.formSID = formSID;
        this.caption = caption;
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
