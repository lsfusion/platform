package lsfusion.gwt.client.form.property.cell.view;

import java.util.function.Consumer;

public interface UpdateContext {
    
    Consumer<Object> getCustomRendererPropertyChange();
    
    boolean isPropertyReadOnly();

    boolean isStaticHeight();

    boolean globalCaptionIsDrawn();
}
