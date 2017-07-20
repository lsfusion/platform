package lsfusion.gwt.base.client.ui;

import com.google.gwt.user.client.ui.*;

public class ResizableTabPanel extends TabPanel implements RequiresResize, ProvidesResize {
    @Override
    public void onResize() {
        for (Widget child : this) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
        }
    }
}
