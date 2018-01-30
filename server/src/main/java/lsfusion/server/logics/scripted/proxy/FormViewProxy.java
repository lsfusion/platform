package lsfusion.server.logics.scripted.proxy;

import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.i18n.LocalizedString;

import javax.swing.*;
import java.awt.*;

public class FormViewProxy extends ViewProxy<FormView> {
    private final ContainerViewProxy mainContainerProxy;

    public FormViewProxy(FormView target) {
        super(target);

        mainContainerProxy = new ContainerViewProxy(target.mainContainer);
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        target.keyStroke = keyStroke;
    }

    public void setCaption(LocalizedString caption) {
        target.caption = caption;
    }

    public void setOverridePageWidth(Integer overridePageWidth) {
        target.overridePageWidth = overridePageWidth;
    }

    /* ========= Redirection to Main Container ========= */

    public void setSize(Dimension size) {
        mainContainerProxy.setSize(size);
    }

    public void setHeight(int height) {
        mainContainerProxy.setHeight(height);
    }

    public void setWidth(int width) {
        mainContainerProxy.setWidth(width);
    }

    public void setColumns(int columns) {
        mainContainerProxy.setColumns(columns);
    }

    public void setMarginTop(int marginTop) {
        mainContainerProxy.setMarginTop(marginTop);
    }

    public void setMarginBottom(int marginBottom) {
        mainContainerProxy.setMarginBottom(marginBottom);
    }

    public void setMarginLeft(int marginLeft) {
        mainContainerProxy.setMarginLeft(marginLeft);
    }

    public void setMarginRight(int marginRight) {
        mainContainerProxy.setMarginRight(marginRight);
    }

    public void setMargin(int margin) {
        mainContainerProxy.setMargin(margin);
    }

    public void setChildrenAlignment(FlexAlignment falign) {
        mainContainerProxy.setChildrenAlignment(falign);
    }

    public void setType(ContainerType type) {
        mainContainerProxy.setType(type);
    }

    public void setDescription(LocalizedString description) {
        mainContainerProxy.setDescription(description);
    }
}
