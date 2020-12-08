package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;

import static lsfusion.gwt.client.base.GwtClientUtils.calculateMaxPreferredSize;

public class ResizableComplexPanel extends ComplexPanel implements RequiresResize, ProvidesResize, HasMaxPreferredSize, ResizableMainPanel {
    private Widget main;

    public ResizableComplexPanel() {
        setElement(Document.get().createDivElement());
    }

    @Override
    public void onResize() {
        if (!visible) {
            return;
        }
        for (Widget child : this) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
        }
    }

    boolean visible = true;
    @Override
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            super.setVisible(visible);
        }
    }

    @Override
    public void add(Widget child) {
        add(child, getElement());
    }

    public void insert(Widget w, int beforeIndex) {
        super.insert(w, getElement(), beforeIndex, true);
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return calculateMaxPreferredSize(main);
    }

    @Override
    public void setMain(Widget main) {
        this.main = main;
        add(main);
    }

    @Override
    public Widget getPanelWidget() {
        return this;
    }
}
