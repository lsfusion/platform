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

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.jsni.JSNIHelper;
import lsfusion.gwt.client.base.jsni.NativeHashMap;

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
     * The attribute used to indicate that an element contains a header.
     */
    private static final String HEADER_ATTRIBUTE = "__gwt_header";


    private final DataGrid<T> table;

    private final TableSectionElement headerElement;

    protected final HeaderDelegate delegate;

    private final NativeHashMap<String, Header<?>> idToHeaderMap = new NativeHashMap<>();

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
        th.setPropertyObject(HEADER_ATTRIBUTE, header);
        header.renderDom(th);
    }

    public Header<?> getHeader(Element elem) {
        return (Header<?>) elem.getPropertyObject(HEADER_ATTRIBUTE);
    }

    /**
     * Renders a given Header into a given TableCellElement.
     */
    protected final <H> void updateHeader(TableCellElement th, Header<H> header) {
        header.updateDom(th);
    }
}
