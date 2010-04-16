package platform.client.form.queries;

import platform.client.logics.ClientPropertyView;

import javax.swing.*;
import java.awt.*;

abstract class ValueLinkView extends JPanel {

    ValueLinkListener listener;

    protected ValueLinkView() {
        setLayout(new BorderLayout());
    }

    public void setListener(ValueLinkListener listener) {
        this.listener = listener;
    }

    abstract void propertyChanged(ClientPropertyView property) ;

    public void stopEditing() {}
}
