package lsfusion.client.form.property.classes.renderer.link;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
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
    protected void drawForeground(Color conditionalForeground) {
        //do nothing
    }

    @Override
    protected void drawBorder(boolean isInFocusedRow, boolean hasFocus) {
        //do nothing
    }

    @Override
    protected void paintAsSelected() {
        //do nothing
    }
}