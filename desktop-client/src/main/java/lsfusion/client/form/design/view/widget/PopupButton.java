package lsfusion.client.form.design.view.widget;

import lsfusion.client.base.view.ClientImages;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientContainer;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

public class PopupButton extends ButtonWidget {
    private static final String THREE_DOTS_ICON_PATH = "threedots.png";

    private ClientFormController formController;
    public PopupButton(ClientFormController formController) {
        super(null, ClientImages.get(THREE_DOTS_ICON_PATH));
        this.formController = formController;
    }

    public void setClickHandler(ClientContainer container, Widget widget) {
        JPopupMenu popup = new JPopupMenu();
        popup.add((Component) widget);

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                formController.setContainerCollapsed(container, true);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                formController.setContainerCollapsed(container, false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                formController.setContainerCollapsed(container, true);
            }
        });

        addActionListener(event -> {
            popup.setLocation(getLocation());
            popup.show(this, popup.getLocation().x + getWidth(), popup.getLocation().y);
        });
    }
}
