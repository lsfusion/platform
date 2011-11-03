package platform.gwt.form.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VStack;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.form.client.ui.GFormController;

public class FormFrame extends HLayout implements EntryPoint {
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GWT.log("Необработанная ошибка в GWT: ", t);
            }
        });

        VStack vs = new VStack();
        vs.setOverflow(Overflow.AUTO);
        vs.setWidth100();
        vs.setHeight100();

        vs.addMember(new GFormController(getFormSID()));
        vs.draw();

        GwtClientUtils.removeLoaderFromHostedPage();
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
