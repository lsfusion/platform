package lsfusion.gwt.client.form.property.cell.view;

import java.util.function.Consumer;

public interface UpdateContext {
    
    void changeProperty(GUserInputResult result);

    default boolean isPropertyReadOnly() { return false; }

    boolean globalCaptionIsDrawn();

    Object getValue();

    default boolean isLoading() { return false; }

    default Object getImage() { return null; }

    boolean isSelectedRow();
    default boolean isSelectedLink() { return isSelectedRow(); }

    default boolean isFocusedColumn() { return false; }

    default String getBackground() { return null; }

    default String getForeground() { return null; }
}
