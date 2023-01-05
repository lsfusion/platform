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
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.size.GSize;

import java.util.List;

/**
 * Default implementation of {@link HeaderBuilder} that renders columns.
 *
 * @param <T> the data type of the table
 */
public abstract class DataGridHeaderBuilder<T> implements HeaderBuilder<T> {

    public interface HeaderDelegate {
        Header<?> getHeader(int index);

        TableSectionElement getHeaderElement();

        void setRowStyle(TableRowElement element);
        void setCellStyle(TableCellElement element);
        void addFirstCellStyle(TableCellElement element);
        void addLastCellStyle(TableCellElement element);
        boolean isFooter();
    }

    /**
     * The attribute used to indicate that an element contains a header.
     */
    private static final String HEADER_ATTRIBUTE = "__gwt_header";


    private final DataGrid<T> table;

    private final TableSectionElement headerElement;

    protected final HeaderDelegate delegate;

    /**
     * Create a new DefaultHeaderBuilder for the header of footer section.
     *
     * @param table    the table being built
     */
    public DataGridHeaderBuilder(DataGrid<T> table, HeaderDelegate delegate) {
        this.delegate = delegate;
        this.table = table;
        this.headerElement = delegate.getHeaderElement();
    }

    private TableRowElement headerRow;
    @Override
    public void update(boolean columnsChanged) {
        if (columnsChanged) {
            if (headerRow != null)
                headerRow.removeFromParent();
            headerRow = headerElement.insertRow(-1);
            delegate.setRowStyle(headerRow);
            buildHeaderImpl(headerRow);

            initArrow(headerRow, delegate.isFooter());
        } else {
            updateHeaderImpl(headerRow);
        }
    }

    private void initArrow(Element parent, boolean bottom) {
        Element button = Document.get().createElement("button");
        button.addClassName("btn");
        button.addClassName("btn-light");
        button.addClassName("btn-sm");
        button.addClassName("arrow");
        button.appendChild(bottom ? StaticImage.CHEVRON_DOWN.createImage() : StaticImage.CHEVRON_UP.createImage());
        GwtClientUtils.setOnClick(button, event -> table.scrollToEnd(bottom));

        Element arrowTH = Document.get().createElement("th");
        arrowTH.addClassName("arrow-th");
        arrowTH.addClassName(bottom ? "bottom-arrow" : "top-arrow");

        Element arrowContainer = Document.get().createElement("div");
        arrowContainer.addClassName("arrow-container");
        arrowContainer.appendChild(button);
        arrowTH.appendChild(arrowContainer);
        parent.appendChild(arrowTH);
    }

    public TableRowElement getHeaderRow() {
        return headerRow;
    }

    protected abstract void buildHeaderImpl(TableRowElement tr);

    protected abstract void updateHeaderImpl(TableRowElement tr);

    @Override
    public void updateStickyLeft(List<Integer> stickyColumns, List<GSize> stickyLefts) {
        updateHeaderStickyLeftImpl(getHeaderRow(), stickyColumns, stickyLefts);
    }

    protected abstract void updateHeaderStickyLeftImpl(TableRowElement tr, List<Integer> stickyColumns, List<GSize> stickyLefts);

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
        header.renderAndUpdateDom(th);
    }

    public Header<?> getHeader(Element elem) {
        return (Header<?>) elem.getPropertyObject(HEADER_ATTRIBUTE);
    }

    /**
     * Renders a given Header into a given TableCellElement.
     */
    protected final <H> void updateHeader(TableCellElement th, Header<H> header) {
        if(header != null) { //footer can be null
            header.updateDom(th);
        }
    }
}
