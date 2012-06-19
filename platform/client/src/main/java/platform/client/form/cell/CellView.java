package platform.client.form.cell;

import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;

public interface CellView {
    JComponent getComponent();
    void setListener(CellViewListener listener);
    void setValue(Object ivalue);
    void forceEdit();
    void setCaption(String caption);
    void setToolTip(String caption);
    void setBackgroundColor(Color background);
    void setForegroundColor(Color foreground);
    void changeViewType(ClassViewType type);
}