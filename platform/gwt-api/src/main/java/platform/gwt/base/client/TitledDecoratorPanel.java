package platform.gwt.base.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.DecoratorPanel;

public class TitledDecoratorPanel extends DecoratorPanel {

    public TitledDecoratorPanel(String title) {
        createBorderTitle(title);

        addStyleDependentName("titled");
    }

    private void createBorderTitle(String title) {
        SpanElement spanElement = Document.get().createSpanElement();
        spanElement.setInnerText(title);

        getCellElement(0, 1).appendChild(spanElement);
    }
}
