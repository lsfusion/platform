package platform.gwt.form.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.widgets.layout.HLayout;
import platform.gwt.form.client.ui.GFormController;

public class FormFrame extends HLayout implements EntryPoint {
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GWT.log("sh: ", t);
            }
        });

        new GFormController(getFormSID()).draw();
    }

    public String getFormSID() {
        try {
            Dictionary dict = Dictionary.getDictionary("parameters");
            if (dict != null) {
                return dict.get("formSID");
            }
        } catch (Exception ignored) {
        }

        try {
            return Window.Location.getParameter("formSID");
        } catch (Exception ignored) {
        }

        return null;
    }
}
