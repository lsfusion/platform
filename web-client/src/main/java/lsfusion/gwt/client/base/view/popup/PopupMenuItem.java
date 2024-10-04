package lsfusion.gwt.client.base.view.popup;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.CaptionHtmlOrTextType;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;

public class PopupMenuItem extends SimplePanel {

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
    public PopupMenuItem() {
        this(createLIElement(), null, null, false);
    }

    //bottom panel
    public PopupMenuItem(PopupMenuItemValue itemValue, Scheduler.ScheduledCommand command, Widget panel, boolean interactive) {
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

    private static LIElement createLIElement(String text, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        // actually here should be setDataHtmlOrText with html coming from SimpleTextBasedCellEditor (in getDisplayString),
        // but a) displayString is always html, because it uses bold styling b) it needs some more refactoring, so we'll leave it this way for now
        GwtClientUtils.initCaptionHtmlOrText(element, CaptionHtmlOrTextType.ASYNCVALUES);
        GwtClientUtils.setCaptionHtmlOrText(element, text);
        GwtClientUtils.addClassName(element, interactive ? "dropdown-item" : "dropdown-item-text");
        return element;
    }

    private static LIElement createLIElement(Widget panel, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        element.appendChild(panel.getElement());
        GwtClientUtils.addClassName(element, interactive ? "dropdown-item" : "dropdown-item-text");
        return element;
    }

    private static LIElement createLIElement() {
        LIElement element = Document.get().createLIElement();

        Element hrElement;
        if(MainFrame.useBootstrap) {
            hrElement = Document.get().createHRElement();
        } else
            hrElement = GwtClientUtils.createHorizontalSeparator().getElement();
        GwtClientUtils.addClassName(hrElement, "dropdown-divider");
        element.appendChild(hrElement);

        return element;
    }
}
