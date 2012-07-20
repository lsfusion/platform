package platform.gwt.view2.panel;

import com.google.gwt.user.client.ui.*;
import platform.gwt.view2.GPropertyDraw;

public class TextBoxPanelRenderer implements PanelRenderer {

    protected final Label label;
    protected final TextBox textBox;
    protected final HorizontalPanel panel;

    public TextBoxPanelRenderer(GPropertyDraw property) {
        label = new Label(property.getCaptionOrEmpty() + ": ");
        textBox = new TextBox();

        panel = new HorizontalPanel();
//        panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        panel.add(label);
        panel.add(textBox);
        panel.setCellVerticalAlignment(label, HasVerticalAlignment.ALIGN_MIDDLE);
    }

    @Override
    public Widget getComponent() {
        return panel;
    }

    @Override
    public void setValue(Object value) {
        textBox.setText(value == null ? "" : value.toString());
    }

    @Override
    public void setTitle(String title) {
        label.setText(title);
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        //todo:
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        //todo:

    }
}
