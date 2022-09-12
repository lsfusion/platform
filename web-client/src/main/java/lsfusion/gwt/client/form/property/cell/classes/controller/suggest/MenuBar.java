package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import lsfusion.gwt.client.base.view.FlexPanel;

import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class MenuBar extends ComplexPanel {

    private ArrayList<MenuItem> items = new ArrayList<>();

    private MenuItem selectedItem;

    public MenuBar() {
        super();
        setElement(createULElement());
        sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEMOVE | Event.ONMOUSEOUT | Event.ONFOCUS | Event.ONKEYDOWN);
    }

    public void clearItems() {
        deselectCurrentItem();
        clear();
        items.clear();
    }

    public void addTextItem(String text) {
        addItem(new MenuItem(null, null, text, false));
    }

    public void addBottomPanelItem(FlexPanel bottomPanel, PopupPanel suggestionPopup) {
        bottomPanel.removeFromParent();
        suggestionPopup.add(bottomPanel);
        addItem(new MenuItem(null, null, bottomPanel, false));
    }

    public void addItem(SuggestOracle.Suggestion suggestion, SuggestBox.SuggestionCallback callback) throws IndexOutOfBoundsException {
        addItem(new MenuItem(suggestion, () -> callback.onSuggestionSelected(suggestion), suggestion.getDisplayString(), true));
    }

    private void addItem(MenuItem menuItem) {
        if (menuItem.interactive) {
            items.add(menuItem);
        }
        add(menuItem, getElement());
    }

    public void moveSelectionDown() {
        if (selectFirstItemIfNoneSelected()) {
            return;
        }

        if (selectedItem != null) {
            int index = items.indexOf(selectedItem) + 1;
            if (index == items.size()) { // we're at the end, loop around to the start
                index = 0;
            }
            selectItem(items.get(index));
        }

    }

    public void moveSelectionUp() {
        if (selectFirstItemIfNoneSelected()) {
            return;
        }

        if (selectedItem != null) {
            int index = items.indexOf(selectedItem) - 1;
            if (index < 0) { // we're at the start, loop around to the end
                index = items.size() - 1;
            }
            selectItem(items.get(index));
        }
    }

    @Override
    public void onBrowserEvent(Event event) {
        MenuItem item = findItem(DOM.eventGetTarget(event));
        switch (DOM.eventGetType(event)) {
            case Event.ONMOUSEDOWN: {
                // Fire an item's command when the user clicks on it.
                if (item != null) {
                    doItemAction(item);
                    stopPropagation(event); //added to save focus on element
                }
                break;
            }

            case Event.ONMOUSEMOVE: {
                if (item != null) {
                    selectItem(item);
                }
                break;
            }

            case Event.ONMOUSEOUT: {
                if (item != null) {
                    selectItem(null);
                }
                break;
            }

            case Event.ONFOCUS: {
                selectFirstItemIfNoneSelected();
                break;
            }

            case Event.ONKEYDOWN: {
                int keyCode = event.getKeyCode();
                boolean isRtl = LocaleInfo.getCurrentLocale().isRTL();
                keyCode = KeyCodes.maybeSwapArrowKeysForRtl(keyCode, isRtl);
                switch (keyCode) {
                    case KeyCodes.KEY_LEFT:
                    case KeyCodes.KEY_RIGHT:
                        selectFirstItemIfNoneSelected();
                        stopPropagation(event);
                        break;
                    case KeyCodes.KEY_UP:
                        moveSelectionUp();
                        stopPropagation(event);
                        break;
                    case KeyCodes.KEY_DOWN:
                        moveSelectionDown();
                        stopPropagation(event);
                        break;
                    case KeyCodes.KEY_ESCAPE:
                        deselectCurrentItem();
                        stopPropagation(event);
                        break;
                    case KeyCodes.KEY_TAB:
                        deselectCurrentItem();
                        break;
                    case KeyCodes.KEY_ENTER:
                        if (!selectFirstItemIfNoneSelected()) {
                            doItemAction(selectedItem);
                            stopPropagation(event);
                        }
                        break;
                }

                break;
            }
        }
        super.onBrowserEvent(event);
    }

    public void selectItem(MenuItem item) {
        if (item != selectedItem) {
            if (selectedItem != null) {
                selectedItem.setSelectionStyle(false);
            }

            if (item != null) {
                item.setSelectionStyle(true);
            }

            selectedItem = item;
        }
    }

    protected SuggestOracle.Suggestion getSelectedItemSuggestion() {
        return selectedItem != null ? selectedItem.suggestion : null;
    }

    void deselectCurrentItem() {
        selectItem(null);
    }

    void doItemAction(final MenuItem item) {
        // Ensure that the item is selected.
        selectItem(item);

        // if the command should be fired and the item has one, fire it
        if (item.command != null) {
            deselectCurrentItem();

            // Fire the item's command. The command must be fired in the same event
            // loop or popup blockers will prevent popups from opening.
            Scheduler.get().scheduleFinally(item.command);
        }
    }

    private MenuItem findItem(Element hItem) {
        for (MenuItem item : items) {
            if (item.getElement().isOrHasChild(hItem)) {
                return item;
            }
        }
        return null;
    }

    private boolean selectFirstItemIfNoneSelected() {
        if (selectedItem == null) {
            return selectFirstItem();
        }
        return false;
    }

    public boolean selectFirstItem() {
        if (!items.isEmpty()) {
            selectItem(items.get(0));
            return true;
        }
        return false;
    }

    private class MenuItem extends SimplePanel {
        private SuggestOracle.Suggestion suggestion;
        private Scheduler.ScheduledCommand command;
        private boolean interactive;

        //menu items
        public MenuItem(SuggestOracle.Suggestion suggestion, Scheduler.ScheduledCommand command, String text, boolean interactive) {
            this(createLIElement(text, interactive), suggestion, command, interactive);
        }

        //bottom panel
        public MenuItem(SuggestOracle.Suggestion suggestion, Scheduler.ScheduledCommand command, FlexPanel panel, boolean interactive) {
            this(createLIElement(panel, interactive), suggestion, command, interactive);
        }

        public MenuItem(LIElement liElement, SuggestOracle.Suggestion suggestion, Scheduler.ScheduledCommand command, boolean interactive) {
            super(liElement);
            this.suggestion = suggestion;
            this.command = command;
            this.interactive = interactive;
        }

        private static final String DEPENDENT_STYLENAME_SELECTED_ITEM = "selected";

        protected void setSelectionStyle(boolean selected) {
            if (selected) {
                addStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
            } else {
                removeStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
            }
        }
    }

    private static UListElement createULElement() {
        UListElement element = Document.get().createULElement();
        element.addClassName("dropdown-menu show");
        return element;
    }

    private static final String STYLENAME_DEFAULT = "item";

    private static LIElement createLIElement(String text, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        element.setInnerHTML(text);
        element.addClassName(interactive ? "dropdown-item" : "dropdown-item-text");
        element.addClassName(STYLENAME_DEFAULT);
        return element;
    }

    private static LIElement createLIElement(FlexPanel panel, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        element.appendChild(panel.getElement());
        element.addClassName(interactive ? "dropdown-item" : "dropdown-item-text");
        element.addClassName(STYLENAME_DEFAULT);
        return element;
    }
}
