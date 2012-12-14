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
package platform.gwt.cellview.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

/**
 * A table column header or footer.
 *
 * @param <H> the {@link platform.gwt.cellview.client.cell.Cell} type
 */
public abstract class Header<H> {

    private Set<String> consumedEvents;

    public Header(String... consumedEvents) {
        if (consumedEvents != null && consumedEvents.length > 0) {
            this.consumedEvents = unmodifiableSet(new HashSet<String>(asList(consumedEvents)));
        }
    }

    public Header(Set<String> consumedEvents) {
        if (consumedEvents != null) {
            this.consumedEvents = unmodifiableSet(consumedEvents);
        }
    }

    public Set<String> getConsumedEvents() {
        return consumedEvents;
    }

    /**
     * Handle a browser event that took place within the header.
     *
     * @param elem    the parent Element
     * @param event   the native browser event
     */
    public void onBrowserEvent(Element elem, NativeEvent event) {
        //do nothing by default
    }

    public abstract void renderDom(TableCellElement th);

    public abstract void updateDom(TableCellElement th);
}
