package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.core.client.Scheduler;
import lsfusion.gwt.client.form.view.FormContainer;

public interface GAsyncFormController {

    FormContainer removeAsyncForm();

    void putAsyncForm(FormContainer container);

    FormContainer removeAsyncClosedForm();

    void putAsyncClosedForm(FormContainer container);

    boolean onServerInvocationResponse(); // true if we need to check for obsolete asyncForms

    boolean canShowDockedModal();

    long getEditRequestIndex(); // needed to dispatch responses after editing started (form embedding)

    void scheduleOpen(Scheduler.ScheduledCommand command);

    void cancelScheduledOpening();
}
