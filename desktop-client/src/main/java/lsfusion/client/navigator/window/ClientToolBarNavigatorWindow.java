package lsfusion.client.navigator.window;

import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.view.ToolBarNavigatorView;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientToolBarNavigatorWindow extends ClientNavigatorWindow {

    public int type;
    public boolean showSelect;

    public int verticalTextPosition;
    public int horizontalTextPosition;

    public int verticalAlignment;
    public int horizontalAlignment;

    public float alignmentY;
    public float alignmentX;

    public ClientToolBarNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);

        type = inStream.readInt();
        showSelect = inStream.readBoolean();

        verticalTextPosition = inStream.readInt();
        horizontalTextPosition = inStream.readInt();

        verticalAlignment = inStream.readInt();
        horizontalAlignment = inStream.readInt();

        alignmentY = inStream.readFloat();
        alignmentX = inStream.readFloat();
    }
    
    public boolean isVertical() {
        return type == SwingConstants.VERTICAL;
    }

    public FlexAlignment getFlexAlignment() {
        return isVertical() ?
                (alignmentY == Component.RIGHT_ALIGNMENT ? FlexAlignment.END : alignmentY == Component.CENTER_ALIGNMENT ? FlexAlignment.CENTER : FlexAlignment.START) :
                (alignmentX == Component.RIGHT_ALIGNMENT ? FlexAlignment.END : alignmentX == Component.CENTER_ALIGNMENT ? FlexAlignment.CENTER : FlexAlignment.START);
    }

    @Override
    public ToolBarNavigatorView createView(INavigatorController controller) {
        return new ToolBarNavigatorView(this, controller);
    }
}
