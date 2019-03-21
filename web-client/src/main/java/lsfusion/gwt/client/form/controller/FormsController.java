package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.shared.navigator.GNavigatorAction;
import lsfusion.gwt.shared.form.GForm;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.shared.navigator.window.GModalityType;

public interface FormsController {

    Widget openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler);

    void select(Widget tabContent);

    void executeNavigatorAction(GNavigatorAction action, NativeEvent nativeEvent);

    void executeNotificationAction(String actionSID, int type);

    void setCurrentForm(String formID);

    void dropCurForm(GFormController form);

}
