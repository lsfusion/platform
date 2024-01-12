package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.form.design.GFont;

public interface RenderContext {

    boolean globalCaptionIsDrawn();

    GFont getFont();

    RendererType getRendererType();

    boolean isInputRemoveAllPMB();

    String getPattern();

    String getRegexp();

    String getRegexpMessage();
}
