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

import com.google.gwt.event.shared.HasHandlers;
import platform.gwt.cellview.client.cell.HasCellPreviewHandlers;

import java.util.List;

/**
 * A view that can display a range of data.
 *
 * @param <T> the data type of each row
 */
public interface HasData<T> extends HasCellPreviewHandlers<T>, HasHandlers {
    /**
     * Get the total count of all rows.
     *
     * @return the total row count
     * @see #setRowCount(int)
     */
    int getRowCount();

    /**
     * Get the row value at the specified visible index. Index 0 corresponds to
     * the first item on the page.
     *
     * @param indexOnPage the index on the page
     * @return the row value
     */
    T getRowValue(int indexOnPage);

    /**
     * <p>
     * Set a values associated with the rows in the visible range.
     * </p>
     * <p>
     * This method <i>does not</i> replace all rows in the display; it replaces
     * the row values starting at the specified start index through the length of
     * the the specified values. You must call {@link #setRowCount(int)} to set
     * the total number of rows in the display. You should also use
     * {@link #setRowCount(int)} to remove rows when the total number of rows
     * decreases.
     * </p>
     *
     * @param start  the start index of the data
     * @param values the values within the range
     */
    void setRowData(int start, List<? extends T> values);
}
