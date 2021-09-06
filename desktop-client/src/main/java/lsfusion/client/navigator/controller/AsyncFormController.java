package lsfusion.client.navigator.controller;

import lsfusion.client.form.view.ClientFormDockable;

public interface AsyncFormController {

    ClientFormDockable removeAsyncForm();

    void putAsyncForm(ClientFormDockable container);

    boolean checkNotCompleted();

    boolean onServerInvocationResponse(); // true if we need to check for obsolete asyncForms

}
