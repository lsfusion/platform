/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.client.HasSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.UIObject;

public class MenuItem extends UIObject implements HasHTML, HasEnabled, HasSafeHtml {

  private static final String DEPENDENT_STYLENAME_SELECTED_ITEM = "selected";
  private static final String DEPENDENT_STYLENAME_DISABLED_ITEM = "disabled";

  private ScheduledCommand command;
  private MenuBar parentMenu;
  private boolean enabled = true;

  MenuItem(@IsSafeHtml String text, boolean asHTML) {
    setElement(DOM.createTD());
    setSelectionStyle(false);

    if (asHTML) {
      setHTML(text);
    } else {
      setText(text);
    }

    getElement().setAttribute("id", DOM.createUniqueId());
    getElement().getStyle().setPadding(2, Style.Unit.PX);
    // Add a11y role "menuitem"
    Roles.getMenuitemRole().set(getElement());
  }

  @Override
  public String getHTML() {
    return getElement().getInnerHTML();
  }

  public MenuBar getParentMenu() {
    return parentMenu;
  }

  public ScheduledCommand getScheduledCommand() {
    return command;
  }

  @Override
  public String getText() {
    return getElement().getInnerText();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled) {
      removeStyleDependentName(DEPENDENT_STYLENAME_DISABLED_ITEM);
    } else {
      addStyleDependentName(DEPENDENT_STYLENAME_DISABLED_ITEM);
    }
    this.enabled = enabled;
  }

  @Override
  public void setHTML(SafeHtml html) {
    setHTML(html.asString());
  }

  @Override
  public void setHTML(@IsSafeHtml String html) {
    getElement().setInnerHTML(html);
  }

  public void setScheduledCommand(ScheduledCommand cmd) {
    command = cmd;
  }

  @Override
  public void setText(String text) {
    getElement().setInnerText(text);
  }

  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
  }

  protected void setSelectionStyle(boolean selected) {
    if (selected) {
      addStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
    } else {
      removeStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
    }
  }

  void setParentMenu(MenuBar parentMenu) {
    this.parentMenu = parentMenu;
  }

  private static final String STYLENAME_DEFAULT = "item";

  private SuggestOracle.Suggestion suggestion;

  public MenuItem(SuggestOracle.Suggestion suggestion, boolean asHTML) {
    this(suggestion.getDisplayString(), asHTML);
    // Each suggestion should be placed in a single row in the suggestion
    // menu. If the window is resized and the suggestion cannot fit on a
    // single row, it should be clipped (instead of wrapping around and
    // taking up a second row).
    getElement().getStyle().setProperty("whiteSpace", "nowrap");
    setStyleName(STYLENAME_DEFAULT);
    setSuggestion(suggestion);
  }

  public SuggestOracle.Suggestion getSuggestion() {
    return suggestion;
  }

  public void setSuggestion(SuggestOracle.Suggestion suggestion) {
    this.suggestion = suggestion;
  }
}