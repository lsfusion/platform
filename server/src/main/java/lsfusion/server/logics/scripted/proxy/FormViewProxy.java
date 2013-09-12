package lsfusion.server.logics.scripted.proxy;

import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.view.FormView;

import javax.swing.*;
import java.awt.*;

public class FormViewProxy extends ViewProxy<FormView> {
    public FormViewProxy(FormView target) {
        super(target);
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        target.keyStroke = keyStroke;
    }

    public void setCaption(String caption) {
        target.caption = caption;
    }

    public void setOverridePageWidth(Integer overridePageWidth) {
        target.overridePageWidth = overridePageWidth;
    }
    
    
    /* ========= Redirection to Main Container ========= */

    public void setDescription(String description) {
        target.mainContainer.description = description;
    }

    public void setType(ContainerType type) {
        target.mainContainer.setType(type);
    }

    public void setChildrenAlignment(FlexAlignment falign) {
        Alignment align;
        switch (falign) {
            case LEADING: align = Alignment.LEADING; break;
            case CENTER: align = Alignment.CENTER; break;
            case TRAILING: align = Alignment.TRAILING; break;
            default:
                throw new IllegalStateException("Children alignment should be either of LEADING, CENTER, TRAILING");
        }
        target.mainContainer.setChildrenAlignment(align);
    }

    public void setColumns(int columns) {
        target.mainContainer.columns = columns;
    }

    public void setGapX(int gapX) {
        target.mainContainer.gapX = gapX;
    }

    public void setGapY(int gapY) {
        target.mainContainer.gapY = gapY;
    }
    
    public void setMinimumSize(Dimension minimumSize) {
        target.mainContainer.minimumSize = minimumSize;
    }

    public void setMinimumHeight(int minHeight) {
        if (target.mainContainer.minimumSize == null) {
            target.mainContainer.minimumSize = new Dimension(-1, minHeight);
        } else {
            target.mainContainer.minimumSize.height = minHeight;
        }
    }

    public void setMinimumWidth(int minWidth) {
        if (target.mainContainer.minimumSize == null) {
            target.mainContainer.minimumSize = new Dimension(minWidth, -1);
        } else {
            target.mainContainer.minimumSize.width = minWidth;
        }
    }

    public void setMaximumSize(Dimension maximumSize) {
        target.mainContainer.maximumSize = maximumSize;
    }

    public void setMaximumHeight(int maxHeight) {
        if (target.mainContainer.maximumSize == null) {
            target.mainContainer.maximumSize = new Dimension(-1, maxHeight);
        } else {
            target.mainContainer.maximumSize.height = maxHeight;
        }
    }

    public void setMaximumWidth(int maxWidth) {
        if (target.mainContainer.maximumSize == null) {
            target.mainContainer.maximumSize = new Dimension(maxWidth, -1);
        } else {
            target.mainContainer.maximumSize.width = maxWidth;
        }
    }

    public void setPreferredSize(Dimension preferredSize) {
        target.mainContainer.preferredSize = preferredSize;
    }

    public void setPreferredHeight(int prefHeight) {
        if (target.mainContainer.preferredSize == null) {
            target.mainContainer.preferredSize = new Dimension(-1, prefHeight);
        } else {
            target.mainContainer.preferredSize.height = prefHeight;
        }
    }

    public void setPreferredWidth(int prefWidth) {
        if (target.mainContainer.preferredSize == null) {
            target.mainContainer.preferredSize = new Dimension(prefWidth, -1);
        } else {
            target.mainContainer.preferredSize.width = prefWidth;
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
}
