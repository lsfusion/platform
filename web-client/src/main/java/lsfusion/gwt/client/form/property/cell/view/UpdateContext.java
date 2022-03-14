package lsfusion.gwt.client.form.property.cell.view;

import java.util.function.Consumer;

public interface UpdateContext {
    
    Consumer<Object> getCustomRendererValueChangeConsumer();
    
    boolean isPropertyReadOnly();

    boolean globalCaptionIsDrawn();

    Object getValue();
}
