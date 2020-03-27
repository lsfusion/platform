package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import org.fit.cssbox.swingbox.BrowserPane;

import java.awt.*;
import java.io.IOException;

public class HTMLLinkPropertyRenderer extends PropertyRenderer {
    private BrowserPane browserPane;

    public HTMLLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    public BrowserPane getComponent() {
        if (browserPane == null) {
            browserPane = new BrowserPane();
        }
        return browserPane;
    }

    @Override
    public void setValue(final Object value) {
        if (value != null) {
            try {
                browserPane.setPage((String) value);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void drawBackground(boolean isInFocusedRow, boolean hasFocus, Color conditionalBackground) {
        //do nothing
    }

    @Override
    protected void drawForeground(boolean isInFocusedRow, boolean hasFocus, Color conditionalForeground) {
        //do nothing
    }

    @Override
    protected void drawBorder(boolean isInFocusedRow, boolean hasFocus, boolean drawFocusBorder) {
        //do nothing
    }

    @Override
    protected void paintAsSelected() {
        //do nothing
    }
}