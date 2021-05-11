package lsfusion.gwt.client.navigator.controller;

import lsfusion.gwt.client.form.view.FormContainer;

public interface GAsyncFormController {

    FormContainer removeAsyncForm();

    void putAsyncForm(FormContainer container);

    boolean checkNotCompleted();

    boolean onServerInvocationResponse(); // true if we need to check for obsolete asyncForms
}
