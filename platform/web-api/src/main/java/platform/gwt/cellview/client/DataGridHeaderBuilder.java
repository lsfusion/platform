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
package platform.gwt.cellview.client;

import com.google.gwt.dom.client.*;
import platform.gwt.base.client.jsni.JSNIHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link HeaderBuilder} that renders columns.
 *
 * @param <T> the data type of the table
 */
public abstract class DataGridHeaderBuilder<T> implements HeaderBuilder<T> {

    public interface HeaderDelegate {
        Header<?> getHeader(int index);

        TableSectionElement getHeaderElement();

        String getCellStyle();

        String getFirstCellStyle();

        String getLastCellStyle();
    }

    /**
     * A map that provides O(1) access to a value given the key, or to the key
     * given the value.
     */
    private static class TwoWayHashMap<K, V> {
        private final Map<K, V> keyToValue = new HashMap<K, V>();
        private final Map<V, K> valueToKey = new HashMap<V, K>();

        void clear() {
            keyToValue.clear();
            valueToKey.clear();
        }

        K getKey(V value) {
            return valueToKey.get(value);
        }

        V getValue(K key) {
            return keyToValue.get(key);
        }

        void put(K key, V value) {
            keyToValue.put(key, value);
            valueToKey.put(value, key);
        }
    }

    /**
     * The attribute used to indicate that an element contains a header.
     */
    private static final String HEADER_ATTRIBUTE = "__gwt_header";


    private final DataGrid<T> table;

    private final TableSectionElement headerElement;

    protected final HeaderDelegate delegate;

    // The following fields are reset on every build.
    private final TwoWayHashMap<String, Header<?>> idToHeaderMap = new TwoWayHashMap<String, Header<?>>();

    /**
     * Create a new DefaultHeaderBuilder for the header of footer section.
     *
     * @param table    the table being built
     * @param isFooter true if building the footer, false if the header
     */
    public DataGridHeaderBuilder(DataGrid<T> table, HeaderDelegate delegate) {
        this.delegate = delegate;
        this.table = table;
        this.headerElement = delegate.getHeaderElement();
    }

    @Override
    public void update(boolean columnsChanged) {
        int rowCount = headerElement.getChildCount();
        if (columnsChanged || rowCount == 0) {
            if (rowCount != 0) {
                assert rowCount == 1;
                headerElement.getRows().getItem(0).removeFromParent();
            }
            buildHeaderImpl(headerElement.insertRow(0));
        } else {
            updateHeaderImpl(headerElement.getRows().getItem(0));
        }
    }

    protected abstract void buildHeaderImpl(TableRowElement tr);

    protected abstract void updateHeaderImpl(TableRowElement tr);

    @Override
    public Header<?> getHeader(Element elem) {
        String headerId = getHeaderId(elem);
        return (headerId == null) ? null : idToHeaderMap.getValue(headerId);
    }

    @Override
    public boolean isHeader(Element elem) {
        return getHeaderId(elem) != null;
    }

    /**
     * Get the header or footer at the specified index.
     *
     * @param index the column index of the header
     * @return the header or footer, depending on the value of isFooter
     */
    protected final Header<?> getHeader(int index) {
        return delegate.getHeader(index);
    }

    protected DataGrid<T> getTable() {
        return table;
    }

    /**
     * Renders a given Header into a given TableCellElement.
     */
    protected final <H> void renderHeader(TableCellElement th, Header<H> header) {
        // Generate a unique ID for the header.
        String headerId = idToHeaderMap.getKey(header);
        if (headerId == null) {
            headerId = "header-" + Document.get().createUniqueId();
            idToHeaderMap.put(headerId, header);
        }
        th.setAttribute(HEADER_ATTRIBUTE, headerId);
        header.renderDom(th);
    }

    /**
     * Renders a given Header into a given TableCellElement.
     */
    protected final <H> void updateHeader(TableCellElement th, Header<H> header) {
        header.updateDom(th);
    }

    private String getHeaderId(Element elem) {
        if (elem == null) {
            return null;
        }
        return JSNIHelper.getAttributeOrNull(elem, HEADER_ATTRIBUTE);
    }
}
