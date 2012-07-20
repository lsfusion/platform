package platform.gwt.view2.panel;

import com.google.gwt.user.client.ui.*;
import platform.gwt.view2.GPropertyDraw;

public class TextPanelRenderer implements PanelRenderer {

    private final Label label;
    private final TextArea textArea;
    private final Panel panel;

    public TextPanelRenderer(GPropertyDraw property) {
        label = new Label(property.getCaptionOrEmpty() + ": ");
        textArea = new TextArea();

        panel = new VerticalPanel();
        panel.add(label);
        panel.add(textArea);
    }

    @Override
    public Widget getComponent() {
        return panel;
    }

    @Override
    public void setValue(Object value) {
        textArea.setText(value.toString());
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
