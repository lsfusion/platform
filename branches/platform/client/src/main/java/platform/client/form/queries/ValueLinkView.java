package platform.client.form.queries;

import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

abstract class ValueLinkView extends JPanel {

    ValueLinkListener listener;

    ValueLinkView() {
        setLayout(new BorderLayout());
    }

    public void setListener(ValueLinkListener listener) {
        this.listener = listener;
    }

    abstract void propertyChanged(ClientPropertyDraw property) ;

    public void stopEditing() {}
}
