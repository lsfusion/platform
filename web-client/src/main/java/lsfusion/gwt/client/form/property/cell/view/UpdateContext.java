package lsfusion.gwt.client.form.property.cell.view;

import java.util.function.Consumer;

public interface UpdateContext {
    
    void changeProperty(GUserInputResult result);

    boolean isPropertyReadOnly();

    boolean globalCaptionIsDrawn();

    Object getValue();

    boolean isLoading();

    Object getImage();

    boolean isSelected();
}
