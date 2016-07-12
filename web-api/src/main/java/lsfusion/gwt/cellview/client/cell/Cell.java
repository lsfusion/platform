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

import java.util.Set;

/**
 * A lightweight representation of a renderable object.
 *
 * <p>
 * Multiple cell widgets or Columns can share a single Cell instance, but there
 * may be implications for certain stateful Cells. Generally, Cells are
 * stateless flyweights that see the world as row values/keys. If a Column
 * contains duplicate row values/keys, the Cell will not differentiate the value
 * in one row versus another. Similarly, if you use a single Cell instance in
 * multiple Columns, the Cells will not differentiate the values coming from one
 * Column versus another.
 * </p>
 *
 * <p>
 * However, some interactive Cells ({@link EditTextCell}, {@link CheckboxCell},
 * {@link TextInputCell}, etc...) have a stateful "pending" state, which is a
 * map of row values/keys to the end user entered pending value. For example, if
 * an end user types a new value in a {@link TextInputCell}, the
 * {@link TextInputCell} maps the "pending value" and associates it with the
 * original row value/key. The next time the Cell Widget renders that row
 * value/key, the Cell renders the pending value instead. This allows
 * applications to refresh the Cell Widget without clearing out all of the end
 * user's pending changes. In subclass of {@link AbstractEditableCell}, the
 * pending state remains until either the original value is updated (a
 * successful commit), or until
 * {@link AbstractEditableCell#clearViewData(Object)} is called (a failed
 * commit).
 * </p>
 *
 * <p>
 * If you share an interactive Cell between two cell widgets (or Columns within
 * the same CellTable), then when the end user updates the pending value in one
 * widget, it will be reflected in the other widget <i>when the other widget is
 * redrawn</i>. You should base your decision on whether or not to share Cell
 * instances on this behavior.
 * </p>
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.cell.CellExample}
 * </p>
 *
 * <p>
 * <span style="color:red;">Warning: The Cell interface may change in subtle but breaking ways as we
 * continuously seek to improve performance. You should always subclass {@link AbstractCell} instead
 * of implementing {@link Cell} directly.</span>
 * </p>
 *
 * @param <C> the type that this Cell represents
 */
public interface Cell<C> {
    /**
     * Contains information about the context of the Cell.
     */
    public static class Context {

        private final int column;
        private final int index;
        private final Object rowValue;

        /**
         * Create a new {@link Context}.
         *
         * @param index    the absolute index of the value
         * @param column   the column index of the cell, or 0
         * @param rowValue the unique key that represents the row value
         */
        public Context(int index, int column, Object rowValue) {
            this.index = index;
            this.column = column;
            this.rowValue = rowValue;
        }

        /**
         * Get the column index of the cell. If the view only contains a single
         * column, this method returns 0.
         *
         * @return the column index of the cell
         */
        public int getColumn() {
            return column;
        }

        /**
         * Get the absolute index of the value.
         *
         * @return the index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Get the key that uniquely identifies the row object.
         *
         * @return the unique key
         */
        public Object getRowValue() {
            return rowValue;
        }
    }

    /**
     * Type of the cell to distinquish between using renderDom and updateDom
     * @param context
     * @return null, if type is constant
     */
    String getCellType(Context context);

    /**
     * Get the set of events that this cell consumes (see
     * {@link com.google.gwt.dom.client.BrowserEvents BrowserEvents} for useful
     * constants). The container that uses this cell should only pass these events
     * to
     * {@link #onBrowserEvent(com.google.gwt.cell.client.ValueUpdater}
     * when the event occurs.
     *
     * <p>
     * The returned value should not be modified, and may be an unmodifiable set.
     * Changes to the return value may not be reflected in the cell.
     * </p>
     *
     * @return the consumed events, or null if no events are consumed
     * @see com.google.gwt.dom.client.BrowserEvents
     */
    Set<String> getConsumedEvents();

    /**
     * Returns true if the cell is currently editing the data identified by the
     * given element and key. While a cell is editing, widgets containing the cell
     * may choose to pass keystrokes directly to the cell rather than using them
     * for navigation purposes.
     *
     * @param context the {@link Context} of the cell
     * @param parent  the parent Element
     * @param value   the value associated with the cell
     * @return true if the cell is in edit mode
     */
    boolean isEditing(Context context, Element parent, C value);

    /**
     * Handle a browser event that took place within the cell. The default
     * implementation returns null.
     *
     * @param context      the {@link Context} of the cell
     * @param parent       the parent Element
     * @param value        the value associated with the cell
     * @param event        the native browser event
     */
    void onBrowserEvent(Context context, Element parent, C value, NativeEvent event);

    void renderDom(Context context, DivElement cellElement, C value);

    void updateDom(Context context, DivElement cellElement, C value);
}
