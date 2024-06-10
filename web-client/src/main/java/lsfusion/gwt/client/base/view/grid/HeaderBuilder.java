/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dom.client.TableRowElement;

import java.util.List;

/**
 * Builds the DOM elements for the header section of a CellTable. It also
 * provides queries on elements in the last DOM subtree that it created.
 *
 * <p>
 * The default implementation used by cell widgets is
 * {@link DefaultHeaderBuilder}.
 * </p>
 *
 * @param <T> the row data type
 */
public interface HeaderBuilder<T> {

    void update(boolean columnsChanged);

    void updateStickyLeft(List<Integer> stickyColumns, List<DataGrid.StickyParams> stickyLefts);
    
    void updateStickedState(List<Integer> stickyColumns, int lastSticked);

    /**
     * If you want to handle browser events using a subclass of {@link Header},
     * implement this method to return the appropriate instance and cell table
     * will forward events originating in the element to the {@link Header}.
     * Return null if events from the element should be discarded.
     *
     * @param elem the element that the contains header
     * @return the immediate {@link Header} contained by the element
     */
    Header<?> getHeader(Element elem);

    TableRowElement getHeaderRow();
}
