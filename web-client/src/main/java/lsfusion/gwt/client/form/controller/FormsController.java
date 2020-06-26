package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.window.GModalityType;

public interface FormsController {

    FormDockable openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, Event initFilterEvent, WindowHiddenHandler hiddenHandler);

    void selectTab(FormDockable form);

    void executeNavigatorAction(GNavigatorAction action, NativeEvent nativeEvent);

    void executeNotificationAction(String actionSID, int type);

    void registerForm(GFormController form, boolean isModal);
    
    void setCurrentForm(GFormController form);

    void unregisterForm(GFormController form, boolean isModal);

}
