package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.design.GFont;

public interface RenderContext {

    boolean globalCaptionIsDrawn();

    default Boolean isPropertyReadOnly() { return false; }

    GFont getFont();

    Widget getPopupOwnerWidget();

    RendererType getRendererType();

    default boolean isTabFocusable() { return false; }

    boolean isInputRemoveAllPMB();

    String getPattern();

    String getRegexp();

    String getRegexpMessage();
}
