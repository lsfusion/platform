package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.ui.GFormController;
import lsfusion.gwt.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.shared.view.GNavigatorAction;
import lsfusion.gwt.shared.view.GForm;
import lsfusion.gwt.client.grid.EditEvent;
import lsfusion.gwt.shared.view.window.GModalityType;

public interface FormsController {

    Widget openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler);

    void select(Widget tabContent);

    void executeNavigatorAction(GNavigatorAction action, NativeEvent nativeEvent);

    void executeNotificationAction(String actionSID, int type);

    void setCurrentForm(String formID);

    void dropCurForm(GFormController form);

}
