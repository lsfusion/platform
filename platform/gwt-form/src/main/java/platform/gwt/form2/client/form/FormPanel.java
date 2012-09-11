package platform.gwt.form2.client.form;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import platform.gwt.form2.client.events.GlobalEventBus;
import platform.gwt.form2.client.events.OpenFormEvent;
import platform.gwt.form2.client.events.OpenFormHandler;
import platform.gwt.form2.client.form.ui.GFormController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormPanel extends SimpleLayoutPanel {
    private final TabLayoutPanel formTabsPanel;

    public FormPanel() {
        formTabsPanel = new TabLayoutPanel(2, Style.Unit.EM);

        add(formTabsPanel);

        GlobalEventBus.addHandler(OpenFormEvent.getType(), new OpenFormHandler() {
            @Override
            public void openForm(OpenFormEvent openFormEvent) {
                FormPanel.this.openForm(openFormEvent.getFormSID(), openFormEvent.getCaption());
            }
        });

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                String formSID = getParameters().get("formSID");
                if (formSID != null) {
                    OpenFormEvent.fireEvent(formSID, null);
                }
            }
        });
    }

    public void openForm(String formSID, String caption) {
        if (!GWT.isScript() || caption == null) {
            caption = (caption == null ? "" : caption) + "(" + formSID + ")";
        }

        GFormController formController = new GFormController(formSID);
        formTabsPanel.add(formController, new TabWidget(caption));
        formTabsPanel.selectTab(formController);
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

    private class TabWidget extends HorizontalPanel {
        private Label label;
        private Button closeButton;

        public TabWidget(String title) {
            label = new Label(title);
            closeButton = new Button("&#215;");
            closeButton.setStyleName("closeTabButton");

            add(label);
            add(closeButton);

            setCellVerticalAlignment(label, ALIGN_MIDDLE);
            setCellVerticalAlignment(closeButton, ALIGN_MIDDLE);

            closeButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    for (int i = 0; i < formTabsPanel.getWidgetCount(); i++) {
                        if (formTabsPanel.getTabWidget(i) == TabWidget.this) {
                            formTabsPanel.remove(i);
                        }
                    }
                }
            });
        }
    }
}
