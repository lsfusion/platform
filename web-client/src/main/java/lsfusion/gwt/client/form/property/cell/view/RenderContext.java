package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.design.GFont;

public interface RenderContext {

    boolean globalCaptionIsDrawn();

    GFont getFont();

    Widget getPopupOwnerWidget();

    RendererType getRendererType();

    boolean isInputRemoveAllPMB();

    String getPattern();

    String getRegexp();

    String getRegexpMessage();
}
