package platform.gwt.form2.shared.view.panel;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.shared.view.GPropertyDraw;

public class ActionPanelRenderer implements PanelRenderer {

    private final ImageButton button;

    public ActionPanelRenderer(final GFormController form, final GPropertyDraw property) {
        button = new ImageButton(property.caption, property.iconPath);
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                form.executeEditAction(property, "change");
            }
        });
    }

    @Override
    public Widget getComponent() {
        return button;
    }

    @Override
    public void setValue(Object value) {
        button.setEnabled(value != null && (Boolean)value);
    }

    @Override
    public void setCaption(String caption) {
        button.setText(caption);
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        button.getElement().getStyle().setBorderColor((String) value);
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        button.getElement().getStyle().setColor((String) value);
    }
}
