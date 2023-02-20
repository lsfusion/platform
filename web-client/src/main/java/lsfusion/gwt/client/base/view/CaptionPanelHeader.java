package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.view.MainFrame;

public class CaptionPanelHeader extends SimpleWidget {

    // header css classes should correspond header classes
    public final static GFlexAlignment HORZ = GFlexAlignment.START;
    public final static GFlexAlignment VERT = GFlexAlignment.CENTER;

    public CaptionPanelHeader() {
        super(MainFrame.useBootstrap ? "h6" : DivElement.TAG);
    }
}
