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
package lsfusion.gwt.cellview.client;

import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.base.client.EscapeUtils;

public class TextHeader extends Header<String> {

    private final String text;

    /**
     * Construct a new TextHeader.
     *
     * @param text the header text as a String
     */
    public TextHeader(String text) {
        super();
        this.text = text;
    }

    @Override
    public void renderDom(TableCellElement th) {
        if (text != null) {
            th.setInnerText(EscapeUtils.unicodeEscape(text));
        }
    }

    public void updateDom(TableCellElement th) {
        //do nothing as text is constant
    }
}
