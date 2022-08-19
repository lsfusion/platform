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

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.client.HasSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.UIObject;

public class MenuItem extends UIObject implements HasHTML, HasSafeHtml {

  private static final String DEPENDENT_STYLENAME_SELECTED_ITEM = "selected";

  private ScheduledCommand command;

  MenuItem(@IsSafeHtml String text) {
    setElement(DOM.createTD());
    setSelectionStyle(false);

    setHTML(text);

    getElement().getStyle().setPadding(2, Style.Unit.PX);
  }

  @Override
  public String getHTML() {
    return getElement().getInnerHTML();
  }

  public ScheduledCommand getScheduledCommand() {
    return command;
  }

  @Override
  public String getText() {
    return getElement().getInnerText();
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

  protected void setSelectionStyle(boolean selected) {
    if (selected) {
      addStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
    } else {
      removeStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
    }
  }

  private static final String STYLENAME_DEFAULT = "item";

  private SuggestOracle.Suggestion suggestion;

  public MenuItem(SuggestOracle.Suggestion suggestion) {
    this(suggestion.getDisplayString());
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