package platform.server.logics.scripted.proxy;

import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.server.form.view.ComponentView;

import java.awt.*;

public class ComponentViewProxy<T extends ComponentView> extends ViewProxy<T> {
    public ComponentViewProxy(T target) {
        super(target);
    }

    public void setMinimumSize(Dimension minimumSize) {
        target.minimumSize = minimumSize;
    }

    public void setMaximumSize(Dimension maximumSize) {
        target.maximumSize = maximumSize;
    }

    public void setPreferredSize(Dimension preferredSize) {
        target.preferredSize = preferredSize;
    }

    public void setFixedSize(Dimension size) {
        setMinimumSize(size);
        setMaximumSize(size);
        setPreferredSize(size);
    }

    public void setDefaultComponent(boolean defaultComponent) {
        target.defaultComponent = defaultComponent;
    }

    /* ========= constraints properties ========= */

    public void setFillVertical(double fillVertical) {
        target.constraints.fillVertical = fillVertical;
    }

    public void setFillHorizontal(double fillHorizontal) {
        target.constraints.fillHorizontal = fillHorizontal;
    }

    public void setMaxVariables(int maxVariables) {
        target.constraints.maxVariables = maxVariables;
    }

    public void setInsetsSibling(Insets insetsSibling) {
        target.constraints.insetsSibling = insetsSibling;
    }

    public void setInsetsInside(Insets insetsInside) {
        target.constraints.insetsInside = insetsInside;
    }

    public void setDirections(SimplexComponentDirections directions) {
        target.constraints.directions = directions;
    }

    public void setChildConstraints(DoNotIntersectSimplexConstraint childConstraints) {
        target.constraints.childConstraints = childConstraints;
    }

    /* ========= design properties ========= */

    public void setFont(Font font) {
        target.design.font = font;
    }

    public void setHeaderFont(Font headerFont) {
        target.design.headerFont = headerFont;
    }

    public void setBackground(Color background) {
        target.design.background = background;
    }

    public void setForeground(Color foreground) {
        target.design.foreground = foreground;
    }

}
