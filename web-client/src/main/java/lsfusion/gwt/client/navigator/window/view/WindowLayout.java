package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Widget;

// what lays the page's windows out: the root WindowsController draws, and the parent a window's element tells whether
// it is shown. The standard layout's elements (WindowElement) and the React component's layout (ReactWindowLayout) are
// its kinds; the React layout is only ever the root, so it is not a WindowElement a split or tabs could hold
public abstract class WindowLayout {
    public abstract Widget getView();

    public void initializeView(WindowsController controller) {}
    public void onAddView(WindowsController controller) {}

    public void storeWindowsSizes(Storage storage) {}
    public void restoreWindowsSizes(Storage storage) {}
    public void resetWindowSize() {}

    public void setWindowVisible(WindowElement window) {}
    public void setWindowInvisible(WindowElement window) {}
}
