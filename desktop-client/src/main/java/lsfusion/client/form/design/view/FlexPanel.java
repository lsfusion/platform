package lsfusion.client.form.design.view;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.base.SwingUtils.overrideSize;

public class FlexPanel extends JPanel {

    private final boolean vertical;

    //BorderLayout
    public FlexPanel() {
        super(new BorderLayout());
        this.vertical = false;
    }

    public FlexPanel(boolean vertical) {
        this(vertical, FlexAlignment.START);
    }

    public FlexPanel(boolean vertical, FlexAlignment alignment) {
        super(null);
        setLayout(new FlexLayout(this, vertical, alignment));
        this.vertical = vertical;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void add(JComponent widget) {
        add(widget, FlexAlignment.START);
    }

    public void add(JComponent widget, FlexAlignment alignment) {
        add(widget, getComponentCount(), alignment);
    }

    public void add(JComponent widget, int beforeIndex, FlexAlignment alignment) {
        add(widget, beforeIndex, alignment, 0, null); // maybe here it also makes sense to set basis to 0 as in addFill, but for now it's used mostly in vertical container for simple components
    }

    public void addFillFlex(JComponent widget, Integer flexBasis) {
        addFill(widget, getComponentCount(), flexBasis);
    }

    public void addFill(JComponent widget, int beforeIndex, Integer flexBasis) {
        add(widget, beforeIndex, FlexAlignment.STRETCH, 1, flexBasis);
    }

    //main add method
    public void add(JComponent widget, int beforeIndex, FlexAlignment alignment, double flex, Integer flexBasis) {
        add(widget, new FlexConstraints(alignment, flex), beforeIndex);
    }

    public static void setBaseSize(JComponent widget, boolean vertical, Integer size) {
        setBaseSize(widget, vertical, size, false);
    }

    public static void setBaseSize(JComponent element, boolean vertical, Integer size, boolean oppositeAndFixed) {

        //todo: в вебе ещё есть clearProperty, непонятно, как это реализовать
//        String propName = vertical ? (oppositeAndFixed ? "height" : "minHeight") : (oppositeAndFixed ? "width" : "minWidth");
//        if(size != null)
//            element.getStyle().setProperty(propName, size + "px");
//        else
//            element.getStyle().clearProperty(propName);

        if(size != null) {
            if (vertical) {
                if (oppositeAndFixed) {
                    element.setSize(new Dimension(element.getSize().width, size)); //set height
                } else {
                    element.setMinimumSize(new Dimension(element.getMinimumSize().width, size)); //set minHeight
                }
            } else {
                if (oppositeAndFixed) {
                    element.setSize(new Dimension(size, element.getSize().height)); //set width
                } else {
                    element.setMinimumSize(new Dimension(size, element.getMinimumSize().height)); //set minWidth
                }
            }
        }
    }

    private Dimension componentSize;

    public void setComponentSize(Dimension componentSize) {
        this.componentSize = componentSize;
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), componentSize);
    }

    public Dimension getMaxPreferredSize() {
        return getPreferredSize();
    }

}
