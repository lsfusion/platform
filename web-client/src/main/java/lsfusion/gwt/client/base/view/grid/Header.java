/*
 * Copyright 2010 Google Inc.
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
package lsfusion.gwt.client.base.view.grid;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;

import static lsfusion.gwt.client.base.EscapeUtils.escapeLineBreakHTML;

/**
 * A table column header or footer.
 */
public abstract class Header<H> {
    private TooltipManager.TooltipHelper tooltipHandler;
    protected String tooltip;

    public Header(GGridPropertyTable table, String tooltip) {
        tooltipHandler = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return Header.this.tooltip;
            }

            @Override
            public boolean stillShowTooltip() {
                return table.isAttached() && table.isVisible();
            }
        };
        this.tooltip = tooltip;
    }

    protected static void renderCaption(Element captionElement, String caption) {
        captionElement.setInnerHTML(caption == null ? "" : escapeLineBreakHTML(caption));
    }

    /**
     * Handle a browser event that took place within the header.
     *
     * @param elem    the parent Element
     * @param event   the native browser event
     */
    public void onBrowserEvent(Element elem, NativeEvent event) {
        TooltipManager.checkTooltipEvent(event, tooltipHandler);
    }

    public abstract void renderAndUpdateDom(TableCellElement th);

    public abstract void updateDom(TableCellElement th);
}
