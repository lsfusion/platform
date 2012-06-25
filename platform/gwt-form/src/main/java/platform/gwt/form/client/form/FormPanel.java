package platform.gwt.form.client.form;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import platform.gwt.form.client.events.GlobalEventBus;
import platform.gwt.form.client.events.OpenFormEvent;
import platform.gwt.form.client.events.OpenFormHandler;
import platform.gwt.form.client.form.ui.GFormController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormPanel extends VLayout {
    TabSet formTabs;

    public FormPanel() {
        formTabs = new TabSet();
        formTabs.setWidth100();
        formTabs.setHeight100();
        formTabs.setCanCloseTabs(true);

        setOverflow(Overflow.VISIBLE);
        setWidth100();
        setHeight100();

        addMember(formTabs);

        GlobalEventBus.addHandler(OpenFormEvent.getType(), new OpenFormHandler() {
            @Override
            public void openForm(OpenFormEvent openFormEvent) {
                FormPanel.this.openForm(openFormEvent.getFormSID(), openFormEvent.getCaption());
            }
        });
    }

    public void openForm(String formSID, String caption) {
        Tab formTab = new Tab();
        formTab.setTitle(caption);

        VLayout mainPane = new VLayout();
        mainPane.setWidth100();
        mainPane.setHeight100();
        mainPane.addMember(new GFormController(formSID));

        formTab.setPane(mainPane);

        formTabs.addTab(formTab);
        formTabs.selectTab(formTab);
    }

    //todo: quick open form from request
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
