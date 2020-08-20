package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.form.design.GFont;

public interface RenderContext {
    Integer getStaticHeight();

    boolean isAlwaysSelected(); // needed for editing object style (on ctrl pressed)

    boolean globalCaptionIsDrawn();

    GFont getFont();
}
