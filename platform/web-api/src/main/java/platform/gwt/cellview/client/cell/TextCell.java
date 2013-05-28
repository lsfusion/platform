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
package platform.gwt.cellview.client.cell;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import platform.gwt.base.client.EscapeUtils;

/**
 * A {@link Cell} used to render text.
 */
public class TextCell extends AbstractCell<String> {

    @Override
    public void renderDom(Context context, DivElement cellElement, String value) {
        if (value != null) {
            cellElement.setInnerText(EscapeUtils.unicodeEscape(value));
        }
    }

    @Override
    public void updateDom(Context context, DivElement cellElement, String value) {
        if (value != null) {
            cellElement.setInnerText(EscapeUtils.unicodeEscape(value));
        } else {
            cellElement.setInnerText("");
        }
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event) {
    }
}
