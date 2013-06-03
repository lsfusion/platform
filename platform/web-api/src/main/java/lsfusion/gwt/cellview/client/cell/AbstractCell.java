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
package lsfusion.gwt.cellview.client.cell;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

/**
 * A default implementation of the {@link Cell} interface.
 *
 * <p>
 * <h3>Examples</h3>
 * <dl>
 * <dt>Read only cell</dt>
 * <dd>{@example com.google.gwt.examples.cell.CellExample}</dd>
 * <dt>Cell with events</dt>
 * <dd>{@example com.google.gwt.examples.cell.CellWithEventsExample}</dd>
 * <dt>Interactive cell</dt>
 * <dd>{@example com.google.gwt.examples.cell.InteractionCellExample}</dd>
 * <dt>Editable cell</dt>
 * <dd>{@example com.google.gwt.examples.cell.EditableCellExample}</dd>
 * </dl>
 * </p>
 *
 * @param <C> the type that this Cell represents
 */
public abstract class AbstractCell<C> implements Cell<C> {

    /**
     * The unmodifiable set of events consumed by this cell.
     */
    private Set<String> consumedEvents;

    /**
     * Construct a new {@link AbstractCell} with the specified consumed events.
     * The input arguments are passed by copy.
     *
     * @param consumedEvents the {@link com.google.gwt.dom.client.BrowserEvents
     *                       events} that this cell consumes
     * @see com.google.gwt.dom.client.BrowserEvents
     */
    public AbstractCell(String... consumedEvents) {
        if (consumedEvents != null && consumedEvents.length > 0) {
            this.consumedEvents = unmodifiableSet(new HashSet<String>(asList(consumedEvents)));
        }
    }

    /**
     * Construct a new {@link AbstractCell} with the specified consumed events.
     *
     * @param consumedEvents the events that this cell consumes
     */
    public AbstractCell(Set<String> consumedEvents) {
        if (consumedEvents != null) {
            this.consumedEvents = unmodifiableSet(consumedEvents);
        }
    }

    public Set<String> getConsumedEvents() {
        return consumedEvents;
    }

    /**
     * Returns false. Subclasses that support editing should override this method
     * to return the current editing status.
     */
    public boolean isEditing(Context context, Element parent, C value) {
        return false;
    }

    @Override
    public String getCellType(Context context) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If you override this method to add support for events, remember to pass the
     * event types that the cell expects into the constructor.
     * </p>
     */
    public abstract void onBrowserEvent(Context context, Element parent, C value, NativeEvent event);

    public abstract void renderDom(Context context, DivElement cellElement, C value);

    public abstract void updateDom(Context context, DivElement cellElement, C value);
}
