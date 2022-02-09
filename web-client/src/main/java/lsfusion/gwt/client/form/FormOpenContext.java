package lsfusion.gwt.client.form;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;

public class FormOpenContext {

    public final GFormController form;
    public final Event event;
    public final EditContext editContext;

    public FormOpenContext(GFormController form, Event event, EditContext editContext) {
        this.form = form;
        this.event = event;
        this.editContext = editContext;
    }
}
