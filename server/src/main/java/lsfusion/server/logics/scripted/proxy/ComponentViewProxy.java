package lsfusion.server.logics.scripted.proxy;

import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.DoNotIntersectSimplexConstraint;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.view.ComponentView;
import lsfusion.server.logics.scripted.Bounds;

import javax.swing.*;
import java.awt.*;

public class ComponentViewProxy<T extends ComponentView> extends ViewProxy<T> {
    public ComponentViewProxy(T target) {
        super(target);
    }

    public void setMinimumSize(Dimension minimumSize) {
        target.minimumSize = minimumSize;
    }

    public void setMinimumHeight(int minHeight) {
        if (target.minimumSize == null) {
            target.minimumSize = new Dimension(-1, minHeight);
        } else {
            target.minimumSize.height = minHeight;
        }
    }

    public void setMinimumWidth(int minWidth) {
        if (target.minimumSize == null) {
            target.minimumSize = new Dimension(minWidth, -1);
        } else {
            target.minimumSize.width = minWidth;
        }
    }

    public void setMaximumSize(Dimension maximumSize) {
        target.maximumSize = maximumSize;
    }

    public void setMaximumHeight(int maxHeight) {
        if (target.maximumSize == null) {
            target.maximumSize = new Dimension(-1, maxHeight);
        } else {
            target.maximumSize.height = maxHeight;
        }
    }

    public void setMaximumWidth(int maxWidth) {
        if (target.maximumSize == null) {
            target.maximumSize = new Dimension(maxWidth, -1);
        } else {
            target.maximumSize.width = maxWidth;
        }
    }

    public void setPreferredSize(Dimension preferredSize) {
        target.preferredSize = preferredSize;
    }

    public void setPreferredHeight(int prefHeight) {
        if (target.preferredSize == null) {
            target.preferredSize = new Dimension(-1, prefHeight);
        } else {
            target.preferredSize.height = prefHeight;
        }
    }

    public void setPreferredWidth(int prefWidth) {
        if (target.preferredSize == null) {
            target.preferredSize = new Dimension(prefWidth, -1);
        } else {
            target.preferredSize.width = prefWidth;
        }
    }

    public void setFixedSize(Dimension size) {
        setMinimumSize(size);
        setMaximumSize(size);
        setPreferredSize(size);
    }

    public void setFixedHeight(int height) {
        setMinimumHeight(height);
        setMaximumHeight(height);
        setPreferredHeight(height);
    }

    public void setFixedWidth(int width) {
        setMinimumWidth(width);
        setMaximumWidth(width);
        setPreferredWidth(width);
    }

    public void setDefaultComponent(boolean defaultComponent) {
        target.defaultComponent = defaultComponent;
    }

    /* ========= constraints properties ========= */

    public void setFill(double fill) {
        setFlex(fill);
        setAlignment(fill == 0 ? FlexAlignment.LEADING : FlexAlignment.STRETCH);
    }

    public void setFlex(double flex) {
        target.flex = flex;
    }

    //todo: remove after refactoring to flex + stretch is complete
    public void setFillHorizontal(double fillX) {
        if (target.getContainer() != null) {
            if (target.getContainer().getType() == ContainerType.CONTAINERV) {
                setAlignment(fillX == 0 ? FlexAlignment.LEADING : FlexAlignment.STRETCH);
            } else if (target.getContainer().getType() == ContainerType.CONTAINERH) {
                setFlex(fillX);
            }
        }
    }

    public void setFillVertical(double fillY) {
        if (target.getContainer() != null) {
            if (target.getContainer().getType() == ContainerType.CONTAINERV) {
                setFlex(fillY);
            } else if (target.getContainer().getType() == ContainerType.CONTAINERH) {
                setAlignment(fillY == 0 ? FlexAlignment.LEADING : FlexAlignment.STRETCH);
            }
        }
    }

    //todo: remove
    public void setInsetsInside(Insets insetsInside) {
//        target.constraints.insetsInside = insetsInside;
    }

    //todo: rename to gap ?
    public void setInsetsSibling(Insets insetsSibling) {
//        target.constraints.insetsSibling = insetsSibling;
    }

    //todo: remove after removed from lsfs
    public void setDirections(Bounds directions) {
//        target.constraints.directions = new SimplexComponentDirections(directions.top, directions.left, directions.bottom, directions.right);
    }

    //todo: remove after removed from lsfs
    public void setChildConstraints(DoNotIntersectSimplexConstraint childConstraints) {
//        target.constraints.childConstraints = childConstraints;
    }

    public void setAlign(FlexAlignment alignment) {
        setAlignment(alignment);
    }

    public void setAlignment(FlexAlignment alignment) {
        target.setAlignment(alignment);
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

    public void setIconPath(String iconPath) {
        target.design.iconPath = iconPath;
        target.design.setImage(new ImageIcon(ComponentView.class.getResource("/images/" + iconPath)));
    }

}
