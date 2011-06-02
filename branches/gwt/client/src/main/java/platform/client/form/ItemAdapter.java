package platform.client.form;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ItemAdapter implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            itemSelected(e);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            itemDeselected(e);
        }
    }

    public void itemSelected(ItemEvent e) { }

    public void itemDeselected(ItemEvent e) { }
}
