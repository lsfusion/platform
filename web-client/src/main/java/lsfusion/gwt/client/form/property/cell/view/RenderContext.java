package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.form.design.GFont;

public interface RenderContext {

    boolean isAlwaysSelected(); // needed for editing object style (on ctrl pressed)

    boolean globalCaptionIsDrawn();

//    boolean isLoading();

    GFont getFont();
}
