package lsfusion.gwt.client.base.view.popup;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.base.view.FlexPanel;

public class PopupMenuItem extends SimplePanel {
    private static final String DEPENDENT_STYLENAME_SELECTED_ITEM = "selected";
    
    private PopupMenuItemValue itemValue;
    private Scheduler.ScheduledCommand command;
    private boolean interactive;

    //menu items
    public PopupMenuItem(PopupMenuItemValue itemValue, Scheduler.ScheduledCommand command, boolean interactive) {
        this(itemValue, command, itemValue.getDisplayString(), interactive);
    }
    public PopupMenuItem(PopupMenuItemValue itemValue, Scheduler.ScheduledCommand command, String text, boolean interactive) {
        this(createLIElement(text, interactive), itemValue, command, interactive);
    }

    //bottom panel
    public PopupMenuItem(PopupMenuItemValue itemValue, Scheduler.ScheduledCommand command, FlexPanel panel, boolean interactive) {
        this(createLIElement(panel, interactive), itemValue, command, interactive);
    }

    public PopupMenuItem(LIElement liElement, PopupMenuItemValue itemValue, Scheduler.ScheduledCommand command, boolean interactive) {
        super(liElement);
        this.itemValue = itemValue;
        this.command = command;
        this.interactive = interactive;
    }

    public PopupMenuItemValue getItemValue() {
        return itemValue;
    }

    public Scheduler.ScheduledCommand getCommand() {
        return command;
    }

    public boolean isInteractive() {
        return interactive;
    }

    protected void setSelectionStyle(boolean selected) {
        if (selected) {
            addStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
        } else {
            removeStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
        }
    }

    private static LIElement createLIElement(String text, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        element.setInnerHTML(text);
        element.addClassName(interactive ? "dropdown-item" : "dropdown-item-text");
        return element;
    }

    private static LIElement createLIElement(FlexPanel panel, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        element.appendChild(panel.getElement());
        element.addClassName(interactive ? "dropdown-item" : "dropdown-item-text");
        return element;
    }
}
