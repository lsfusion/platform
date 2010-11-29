package platform.client.form;

import platform.client.logics.ClientContainer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

class ClientFormContainer extends JPanel implements AutoHideableContainer {

    private final ClientContainer key;

    public ClientFormContainer(ClientContainer key) {

        this.key = key;

        setOpaque(false);

        key.design.designComponent(this);
    }

    @Override
    public void add(Component comp, Object constraints) {
        SimplexLayout.showHideableContainers(this);
        super.add(comp, constraints);
    }

    @Override
    public String toString() {
        return key.toString();
    }

    public String getTitle() {
        return key.getTitle();
    }

    public void addBorder() {
        String title = getTitle();
        if (title != null) {
            TitledBorder border = BorderFactory.createTitledBorder(title);
            setBorder(border);
        }
    }
}
