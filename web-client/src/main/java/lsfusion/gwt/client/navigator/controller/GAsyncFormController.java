package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.core.client.Scheduler;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;

public interface GAsyncFormController {

    FormContainer removeAsyncForm();

    void putAsyncForm(FormContainer container);

    Pair<FormDockable, Integer> removeAsyncClosedForm();

    void putAsyncClosedForm(Pair<FormDockable, Integer> container);

    boolean onServerInvocationOpenResponse(); // true if we need to check for obsolete asyncOpenForms

    boolean onServerInvocationCloseResponse(); // true if we need to check for obsolete asyncCloseForms

    boolean canShowDockedModal();

    long getEditRequestIndex(); // needed to dispatch responses after editing started (form embedding)

    GwtActionDispatcher getDispatcher();

    void scheduleOpen(Scheduler.ScheduledCommand command);

    void cancelScheduledOpening();
}
