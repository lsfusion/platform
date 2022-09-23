package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.base.view.FlexPanel;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class PopupPanel extends ComplexPanel {

    private ArrayList<MenuItem> items = new ArrayList<>();

    private MenuItem selectedItem;

    private boolean showing;

    private List<Element> autoHidePartners = new ArrayList<>();

    // the left style attribute in pixels
    private int leftPosition = -1;

    private HandlerRegistration nativePreviewHandlerRegistration;

    // The top style attribute in pixels
    private int topPosition = -1;

    public PopupPanel() {
        super();

        setElement(createULElement());
        sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEMOVE | Event.ONMOUSEOUT | Event.ONFOCUS | Event.ONKEYDOWN);

        // Default position of popup should be in the upper-left corner of the
        // window. By setting a default position, the popup will not appear in
        // an undefined location if it is shown before its position is set.
        setPopupPosition(0, 0);
    }

    public void addAutoHidePartner(Element partner) {
        autoHidePartners.add(partner);
    }

    public void hide() {
        if (!isShowing()) {
            return;
        }
        setState(false);
    }

    public boolean isShowing() {
        return showing;
    }

    /**
     * Remove an autoHide partner.
     *
     * @param partner the auto hide partner to remove
     */
    public void removeAutoHidePartner(Element partner) {
        autoHidePartners.remove(partner);
    }

    /**
     * Sets the popup's position relative to the browser's client area. The
     * popup's position may be set before calling {@link #show()}.
     *
     * @param left the left position, in pixels
     * @param top  the top position, in pixels
     */
    public void setPopupPosition(int left, int top) {
        // Save the position of the popup
        leftPosition = left;
        topPosition = top;

        // Account for the difference between absolute position and the
        // body's positioning context.
        left -= Document.get().getBodyOffsetLeft();
        top -= Document.get().getBodyOffsetTop();

        // Set the popup's position manually, allowing setPopupPosition() to be
        // called before show() is called (so a popup can be positioned without it
        // 'jumping' on the screen).
        Element elem = getElement();
        elem.getStyle().setPropertyPx("left", left);
        elem.getStyle().setPropertyPx("top", top);
    }

    public void setPopupPositionAndShow(final Element target) {
        getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
        show();
        position(target, getOffsetWidth(), getOffsetHeight());
        getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
    }

    /**
     * Shows the popup and attach it to the page. It must have a child widget
     * before this method is called.
     */
    public void show() {
        if (showing) {
            return;
        } else if (isAttached()) {
            // The popup is attached directly to another panel, so we need to remove
            // it from its parent before showing it. This is a weird use case, but
            // since PopupPanel is a Widget, its legal.
            this.removeFromParent();
        }
        setState(true);
    }

    /**
     * Does the event target one of the partner elements?
     *
     * @param event the native event
     * @return true if the event targets a partner
     */
    private boolean eventTargetsPartner(NativeEvent event) {
        EventTarget target = event.getEventTarget();
        if (Element.is(target)) {
            for (Element elem : autoHidePartners) {
                if (elem.isOrHasChild(Element.as(target))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the event target this popup?
     *
     * @param event the native event
     * @return true if the event targets the popup
     */
    private boolean eventTargetsPopup(NativeEvent event) {
        EventTarget target = event.getEventTarget();
        if (Element.is(target)) {
            return getElement().isOrHasChild(Element.as(target));
        }
        return false;
    }

    boolean popupIsAbove;

    /**
     * Positions the popup, called after the offset width and height of the popup
     * are known.
     *
     * @param relativeObject the ui object to position relative to
     * @param offsetWidth    the drop down's offset width
     * @param offsetHeight   the drop down's offset height
     */
    private void position(final Element relativeObject, int offsetWidth, int offsetHeight) {
        // Calculate left position for the popup. The computation for
        // the left position is bidi-sensitive.

        int textBoxOffsetWidth = relativeObject.getOffsetWidth();

        // Compute the difference between the popup's width and the
        // textbox's width
        int offsetWidthDiff = offsetWidth - textBoxOffsetWidth;

        int left;

        if (LocaleInfo.getCurrentLocale().isRTL()) { // RTL case

            int textBoxAbsoluteLeft = relativeObject.getAbsoluteLeft();

            // Right-align the popup. Note that this computation is
            // valid in the case where offsetWidthDiff is negative.
            left = textBoxAbsoluteLeft - offsetWidthDiff;

            // If the suggestion popup is not as wide as the text box, always
            // align to the right edge of the text box. Otherwise, figure out whether
            // to right-align or left-align the popup.
            if (offsetWidthDiff > 0) {

                // Make sure scrolling is taken into account, since
                // box.getAbsoluteLeft() takes scrolling into account.
                int windowRight = Window.getClientWidth() + Window.getScrollLeft();
                int windowLeft = Window.getScrollLeft();

                // Compute the left value for the right edge of the textbox
                int textBoxLeftValForRightEdge = textBoxAbsoluteLeft + textBoxOffsetWidth;

                // Distance from the right edge of the text box to the right edge
                // of the window
                int distanceToWindowRight = windowRight - textBoxLeftValForRightEdge;

                // Distance from the right edge of the text box to the left edge of the
                // window
                int distanceFromWindowLeft = textBoxLeftValForRightEdge - windowLeft;

                // If there is not enough space for the overflow of the popup's
                // width to the right of the text box and there IS enough space for the
                // overflow to the right of the text box, then left-align the popup.
                // However, if there is not enough space on either side, stick with
                // right-alignment.
                if (distanceFromWindowLeft < offsetWidth && distanceToWindowRight >= offsetWidthDiff) {
                    // Align with the left edge of the text box.
                    left = textBoxAbsoluteLeft;
                }
            }
        } else { // LTR case

            // Left-align the popup.
            left = relativeObject.getAbsoluteLeft();

            // If the suggestion popup is not as wide as the text box, always align to
            // the left edge of the text box. Otherwise, figure out whether to
            // left-align or right-align the popup.
            if (offsetWidthDiff > 0) {
                // Make sure scrolling is taken into account, since
                // box.getAbsoluteLeft() takes scrolling into account.
                int windowRight = Window.getClientWidth() + Window.getScrollLeft();
                int windowLeft = Window.getScrollLeft();

                // Distance from the left edge of the text box to the right edge
                // of the window
                int distanceToWindowRight = windowRight - left;

                // Distance from the left edge of the text box to the left edge of the
                // window
                int distanceFromWindowLeft = left - windowLeft;

                // If there is not enough space for the overflow of the popup's
                // width to the right of hte text box, and there IS enough space for the
                // overflow to the left of the text box, then right-align the popup.
                // However, if there is not enough space on either side, then stick with
                // left-alignment.
                if (distanceToWindowRight < offsetWidth && distanceFromWindowLeft >= offsetWidthDiff) {
                    // Align with the right edge of the text box.
                    left -= offsetWidthDiff;
                }
            }
        }

        // Calculate top position for the popup

        int top = relativeObject.getAbsoluteTop();

        // Make sure scrolling is taken into account, since
        // box.getAbsoluteTop() takes scrolling into account.
        int windowTop = Window.getScrollTop();
        int windowBottom = Window.getScrollTop() + Window.getClientHeight();

        // Distance from the top edge of the window to the top edge of the
        // text box
        int distanceFromWindowTop = top - windowTop;

        // Distance from the bottom edge of the window to the bottom edge of
        // the text box
        int distanceToWindowBottom = windowBottom - (top + relativeObject.getOffsetHeight());

        // If there is not enough space for the popup's height below the text
        // box and there IS enough space for the popup's height above the text
        // box, then position the popup above the text box. However, if there
        // is not enough space on either side, then stick with displaying the
        // popup below the text box.
        if (distanceToWindowBottom < offsetHeight && distanceFromWindowTop >= offsetHeight) {
            top -= offsetHeight;
            popupIsAbove = true;
        } else {
            // Position above the text box
            top += relativeObject.getOffsetHeight();
            popupIsAbove = false;
        }
        setPopupPosition(left, top);
    }

    private void previewNativeEvent(NativePreviewEvent event) {
        // If the event has been canceled or consumed, ignore it
        if (event.isCanceled()) {
            // We need to ensure that we cancel the event even if its been consumed so
            // that popups lower on the stack do not auto hide
            return;
        }

        if (event.isCanceled()) {
            return;
        }

        // If the event targets the popup or the partner, consume it
        Event nativeEvent = Event.as(event.getNativeEvent());

        boolean eventTargetsPopupOrPartner = eventTargetsPopup(nativeEvent) || eventTargetsPartner(nativeEvent);
        if (eventTargetsPopupOrPartner) {
            event.consume();
        }

        // Switch on the event type
        int type = nativeEvent.getTypeInt();
        switch (type) {
            case Event.ONKEYDOWN:
            case Event.ONKEYPRESS:
            case Event.ONKEYUP: {
                return;
            }

            case Event.ONMOUSEDOWN:
            case Event.ONTOUCHSTART:
                // Don't eat events if event capture is enabled, as this can
                // interfere with dialog dragging, for example.
                if (DOM.getCaptureElement() != null) {
                    event.consume();
                    return;
                }

                if (!eventTargetsPopupOrPartner) {
                    hide();
                    return;
                }
                break;
            case Event.ONMOUSEUP:
            case Event.ONMOUSEMOVE:
            case Event.ONCLICK:
            case Event.ONDBLCLICK:
            case Event.ONTOUCHEND: {
                // Don't eat events if event capture is enabled, as this can
                // interfere with dialog dragging, for example.
                if (DOM.getCaptureElement() != null) {
                    event.consume();
                    return;
                }
                break;
            }

            case Event.ONFOCUS: {
                break;
            }
        }
    }

    private void updateHandlers() {
        // Remove any existing handlers.
        if (nativePreviewHandlerRegistration != null) {
            nativePreviewHandlerRegistration.removeHandler();
            nativePreviewHandlerRegistration = null;
        }

        // Create handlers if showing.
        if (showing) {
            nativePreviewHandlerRegistration = Event.addNativePreviewHandler(this::previewNativeEvent);
        }
    }

    public void clearItems() {

        // Hide the popup before we manipulate the menu within it. If we do not
        // do this, some browsers will redraw the popup as items are removed
        // and added to the menu.
        if (isAttached()) {
            hide();
        }

        clearSelectedItem();
        clear();
        items.clear();
    }

    public void addTextItem(String text) {
        addItem(new MenuItem(null, null, text, false));
    }

    public void addBottomPanelItem(FlexPanel bottomPanel) {
        bottomPanel.removeFromParent();
        add(bottomPanel, getElement());
        addItem(new MenuItem(null, null, bottomPanel, false));
    }

    public void addItem(SuggestBox.Suggestion suggestion, SuggestBox.SuggestionCallback callback) throws IndexOutOfBoundsException {
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
                    clearSelectedItem();
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
                        clearSelectedItem();
                        stopPropagation(event);
                        break;
                    case KeyCodes.KEY_TAB:
                        clearSelectedItem();
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

    protected SuggestBox.Suggestion getSelectedItemSuggestion() {
        return selectedItem != null ? selectedItem.suggestion : null;
    }

    void clearSelectedItem() {
        selectItem(null);
    }

    void doItemAction(final MenuItem item) {
        // Ensure that the item is selected.
        selectItem(item);

        // if the command should be fired and the item has one, fire it
        if (item.command != null) {
            clearSelectedItem();

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
            return popupIsAbove ? selectLastItem() : selectFirstItem();
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

    public boolean selectLastItem() {
        if (!items.isEmpty()) {
            selectItem(items.get(items.size() - 1));
            return true;
        }
        return false;
    }

    public void setState(boolean showing) {
        // Update the logical state.
        this.showing = showing;
        updateHandlers();

        if (showing) {
            // Set the position attribute, and then attach to the DOM. Otherwise,
            // the PopupPanel will appear to 'jump' from its static/relative
            // position to its absolute position (issue #1231).
            getElement().getStyle().setProperty("position", "absolute");
            if (topPosition != -1) {
                setPopupPosition(leftPosition, topPosition);
            }
            RootPanel.get().add(this);
        } else {
            RootPanel.get().remove(this);
        }
        getElement().getStyle().setProperty("overflow", "visible");

    }

    private class MenuItem extends SimplePanel {
        private SuggestBox.Suggestion suggestion;
        private Scheduler.ScheduledCommand command;
        private boolean interactive;

        //menu items
        public MenuItem(SuggestBox.Suggestion suggestion, Scheduler.ScheduledCommand command, String text, boolean interactive) {
            this(createLIElement(text, interactive), suggestion, command, interactive);
        }

        //bottom panel
        public MenuItem(SuggestBox.Suggestion suggestion, Scheduler.ScheduledCommand command, FlexPanel panel, boolean interactive) {
            this(createLIElement(panel, interactive), suggestion, command, interactive);
        }

        public MenuItem(LIElement liElement, SuggestBox.Suggestion suggestion, Scheduler.ScheduledCommand command, boolean interactive) {
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

    private UListElement createULElement() {
        UListElement element = Document.get().createULElement();
        element.addClassName("dropdown-menu show");
        return element;
    }

    private LIElement createLIElement(String text, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        element.setInnerHTML(text);
        element.addClassName(interactive ? "dropdown-item" : "dropdown-item-text");
        return element;
    }

    private LIElement createLIElement(FlexPanel panel, boolean interactive) {
        LIElement element = Document.get().createLIElement();
        element.appendChild(panel.getElement());
        element.addClassName(interactive ? "dropdown-item" : "dropdown-item-text");
        return element;
    }
}
