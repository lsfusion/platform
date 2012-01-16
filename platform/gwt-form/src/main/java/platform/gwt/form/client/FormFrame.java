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

import java.util.*;

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

        vs.addMember(new GFormController(getParameters()));
        vs.draw();

        GwtClientUtils.removeLoaderFromHostedPage();
    }

    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<String, String>();
        try {
            Dictionary dict = Dictionary.getDictionary("parameters");
            if (dict != null) {
                for (String param : dict.keySet()) {
                    params.put(param, dict.get(param));
                }
                return params;
            }
        } catch (Exception ignored) {
        }

        try {
            Map<String, List<String>> paramMap = Window.Location.getParameterMap();
            for (String param : paramMap.keySet()) {
                params.put(param, paramMap.get(param).isEmpty() ? null : paramMap.get(param).get(0));
            }
        } catch (Exception ignored) {
        }

        return params;
    }
}
