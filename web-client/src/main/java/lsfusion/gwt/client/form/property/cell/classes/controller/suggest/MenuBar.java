/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.FocusImpl;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class MenuBar extends Widget implements HasAnimation {

  static final FocusImpl focusImpl = FocusImpl.getFocusImplForPanel();

  private ArrayList<UIObject> allItems = new ArrayList<>();

  private ArrayList<MenuItem> items = new ArrayList<>();

  private Element body;

  private boolean isAnimationEnabled = false;
  private MenuItem selectedItem;

  public MenuBar() {
    init();
  }

  public void addItem(MenuItem item) {
    insertItem(item, allItems.size());
  }

  public void clearItems() {
    deselectCurrentItem();

    Element container = getItemContainerElement();
    while (DOM.getChildCount(container) > 0) {
      container.removeChild(DOM.getChild(container, 0));
    }

    // Clear out all of the items and separators
    items.clear();
    allItems.clear();
  }

  public void setFocus(boolean focus) {
    // in Firefox FocusImpl calls focus() immediately
    // (in suggest box blur event is called before menu item select action, which leads to commit editing problems)
    // while in FocusImplSafari (Chrome) this is done with 0 delay timeout.
    // doing the same here for equal behavior (see also TextBasedCellEditor.setFocus())
    Timer t = new Timer() {
      public void run() {
        if (focus) {
          focusImpl.focus(getElement());
        } else {
          focusImpl.blur(getElement());
        }
      }
    };
    t.schedule(0);
  }

  public void insertItem(MenuItem item, int beforeIndex)
      throws IndexOutOfBoundsException {
    // Check the bounds
    if (beforeIndex < 0 || beforeIndex > allItems.size()) {
      throw new IndexOutOfBoundsException();
    }

    // Add to the list of items
    allItems.add(beforeIndex, item);
    int itemsIndex = 0;
    for (int i = 0; i < beforeIndex; i++) {
      if (allItems.get(i) instanceof MenuItem) {
        itemsIndex++;
      }
    }
    items.add(itemsIndex, item);

    // Setup the menu item
    addItemElement(beforeIndex, item.getElement());
    item.setSelectionStyle(false);
  }

  @Override
  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  public void moveSelectionDown() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }
    selectNextItem();
  }

  public void moveSelectionUp() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }
    selectPrevItem();
  }

  @Override
  public void onBrowserEvent(Event event) {
    MenuItem item = findItem(DOM.eventGetTarget(event));
    switch (DOM.eventGetType(event)) {
      //replaced ONCLICK to ONMOUSEDOWN
      case Event.ONMOUSEDOWN: {
        setFocus(true);
        // Fire an item's command when the user clicks on it.
        if (item != null) {
          doItemAction(item);
          stopPropagation(event); //added to save focus on element
        }
        break;
      }

      //replaced ONMOUSEOVER to ONMOUSEMOVE
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
        } // end switch(keyCode)

        break;
      } // end case Event.ONKEYDOWN
    } // end switch (DOM.eventGetType(event))
    super.onBrowserEvent(event);
  }

  public void selectItem(MenuItem item) {
    if (item == selectedItem) {
      return;
    }

    if (selectedItem != null) {
      selectedItem.setSelectionStyle(false);
    }

    if (item != null) {
      item.setSelectionStyle(true);
    }

    selectedItem = item;
  }

  @Override
  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  /**
   * Returns a list containing the <code>MenuItem</code> objects in the menu
   * bar. If there are no items in the menu bar, then an empty <code>List</code>
   * object will be returned.
   *
   * @return a list containing the <code>MenuItem</code> objects in the menu bar
   */
  protected List<MenuItem> getItems() {
    return this.items;
  }

  /**
   * Returns the <code>MenuItem</code> that is currently selected (highlighted)
   * by the user. If none of the items in the menu are currently selected, then
   * <code>null</code> will be returned.
   *
   * @return the <code>MenuItem</code> that is currently selected, or
   *         <code>null</code> if no items are currently selected
   */
  protected MenuItem getSelectedItem() {
    return this.selectedItem;
  }

  void deselectCurrentItem() {
    selectItem(null);
  }

  void doItemAction(final MenuItem item) {
    // Ensure that the item is selected.
    selectItem(item);

    // if the command should be fired and the item has one, fire it
    if (item.getScheduledCommand() != null) {
      deselectCurrentItem();

      // Remove the focus from the menu
      setFocus(false);

      // Fire the item's command. The command must be fired in the same event
      // loop or popup blockers will prevent popups from opening.
      final ScheduledCommand cmd = item.getScheduledCommand();
      Scheduler.get().scheduleFinally(cmd);
    }
  }

  /**
   * Physically add the td element of a {@link MenuItem}.
   *
   * @param beforeIndex the index where the separator should be inserted
   * @param tdElem the td element to be added
   */
  private void addItemElement(int beforeIndex, Element tdElem) {
      Element tr = DOM.createTR();
      DOM.insertChild(body, tr, beforeIndex);
      DOM.appendChild(tr, tdElem);
  }

  private MenuItem findItem(Element hItem) {
    for (MenuItem item : items) {
      if (item.getElement().isOrHasChild(hItem)) {
        return item;
      }
    }
    return null;
  }

  private Element getItemContainerElement() {
    return body;
  }

  private void init() {
    Element table = DOM.createTable();
    //override default border-spacing: 2px
    table.getStyle().setProperty("borderSpacing", "0px");
    body = DOM.createTBody();
    DOM.appendChild(table, body);

    Element outer = focusImpl.createFocusable();
    DOM.appendChild(outer, table);
    setElement(outer);

    Roles.getMenubarRole().set(getElement());

    //replaced ONCLICK to ONMOUSEDOWN, ONMOUSEOVER to ONMOUSEMOVE
    sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEMOVE | Event.ONMOUSEOUT | Event.ONFOCUS | Event.ONKEYDOWN);

    // Hide focus outline in Mozilla/Webkit
    getElement().getStyle().setProperty("outline", "0px");
  }

  /**
   * Selects the first item in the menu if no items are currently selected. Has
   * no effect if there are no items.
   *
   * @return true if no item was previously selected, false otherwise
   */
  private boolean selectFirstItemIfNoneSelected() {
    if (selectedItem == null) {
      for (MenuItem nextItem : items) {
        selectItem(nextItem);
        break;
      }
      return true;
    }
    return false;
 }

  private void selectNextItem() {
    if (selectedItem == null) {
      return;
    }

    int index = items.indexOf(selectedItem);
    // We know that selectedItem is set to an item that is contained in the items collection.
    // Therefore, we know that index can never be -1.
    assert (index != -1);

    index = index + 1;
    if (index == items.size()) { // we're at the end, loop around to the start
        index = 0;
    }
    selectItem(items.get(index));
  }

  private void selectPrevItem() {
    if (selectedItem == null) {
      return;
    }

    int index = items.indexOf(selectedItem);
    // We know that selectedItem is set to an item that is contained in the
    // items collection.
    // Therefore, we know that index can never be -1.
    assert (index != -1);

    index = index - 1;
    if (index < 0) { // we're at the start, loop around to the end
        index = items.size() - 1;
    }
    selectItem(items.get(index));
  }

  public int getSelectedItemIndex() {
    // The index of the currently selected item can only be
    // obtained if the menu is showing.
    MenuItem selectedItem = getSelectedItem();
    if (selectedItem != null) {
      return getItems().indexOf(selectedItem);
    }
    return -1;
  }

  public void selectItem(int index) {
    List<MenuItem> items = getItems();
    if (index > -1 && index < items.size()) {
      selectItem(items.get(index));
    }
  }
}
