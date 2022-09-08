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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SuggestOracle;
import lsfusion.gwt.client.base.view.FlexPanel;

import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class MenuBar extends FlexPanel {

  private ArrayList<MenuItem> items = new ArrayList<>();

  private MenuItem selectedItem;

  public MenuBar() {
    super(true);
    sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEMOVE | Event.ONMOUSEOUT | Event.ONFOCUS | Event.ONKEYDOWN);
  }

  public void clearItems() {
    deselectCurrentItem();
    clear();
    items.clear();
  }

  public void addItem(SuggestOracle.Suggestion suggestion, SuggestBox.SuggestionCallback callback) throws IndexOutOfBoundsException {
    final MenuItem menuItem = new MenuItem(suggestion);
    menuItem.setScheduledCommand(() -> callback.onSuggestionSelected(suggestion));

    // Add to the list of items
    items.add(menuItem);

    // Setup the menu item
    add(menuItem);
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
    return selectedItem != null ? selectedItem.getSuggestion() : null;
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

      // Fire the item's command. The command must be fired in the same event
      // loop or popup blockers will prevent popups from opening.
      Scheduler.get().scheduleFinally(item.getScheduledCommand());
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

  private class MenuItem extends HTML {

    private static final String STYLENAME_DEFAULT = "item";

    public MenuItem(SuggestOracle.Suggestion suggestion) {
      super(suggestion.getDisplayString());
      setStyleName(STYLENAME_DEFAULT);
      setSuggestion(suggestion);
    }

    private Scheduler.ScheduledCommand command;

    public Scheduler.ScheduledCommand getScheduledCommand() {
      return command;
    }

    public void setScheduledCommand(Scheduler.ScheduledCommand cmd) {
      command = cmd;
    }

    private static final String DEPENDENT_STYLENAME_SELECTED_ITEM = "selected";
    protected void setSelectionStyle(boolean selected) {
      if (selected) {
        addStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
      } else {
        removeStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
      }
    }

    private SuggestOracle.Suggestion suggestion;

    public SuggestOracle.Suggestion getSuggestion() {
      return suggestion;
    }

    public void setSuggestion(SuggestOracle.Suggestion suggestion) {
      this.suggestion = suggestion;
    }
  }
}
