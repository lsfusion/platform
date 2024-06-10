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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.List;

/**
 * Default implementation of {@link HeaderBuilder} that renders columns.
 *
 * @param <T> the data type of the table
 */
public class DefaultHeaderBuilder<T> extends DataGridHeaderBuilder<T> {
    public static final class HeaderDelegateImpl implements HeaderDelegate {
        private DataGrid grid;

        public HeaderDelegateImpl(DataGrid grid) {
            this.grid = grid;
        }

        @Override
        public Header<?> getHeader(int index) {
            return grid.getHeader(index);
        }

        @Override
        public TableSectionElement getHeaderElement() {
            return grid.getTableHeadElement();
        }

        @Override
        public void setRowStyle(TableRowElement element) {
            element.setClassName("dataGridHeaderRow");
        }

        @Override
        public void setCellStyle(TableCellElement element) {
            element.setClassName("dataGridHeaderCell");
        }

        @Override
        public void addFirstCellStyle(TableCellElement element) {
            element.addClassName("dataGridFirstHeaderCell");
        }

        @Override
        public void addLastCellStyle(TableCellElement element) {
            element.addClassName("dataGridLastHeaderCell");
        }

        @Override
        public boolean isFooter() {
            return false;
        }
    }

    public static final class FooterDelegateImpl implements HeaderDelegate {
        private DataGrid grid;

        public FooterDelegateImpl(DataGrid grid) {
            this.grid = grid;
        }

        @Override
        public Header<?> getHeader(int index) {
            return grid.getFooter(index);
        }

        @Override
        public TableSectionElement getHeaderElement() {
            return grid.getTableFootElement();
        }

        @Override
        public void setRowStyle(TableRowElement element) {
            element.setClassName("dataGridFooterRow");
        }

        @Override
        public void setCellStyle(TableCellElement element) {
            element.setClassName("dataGridFooterCell");
        }

        @Override
        public void addFirstCellStyle(TableCellElement element) {
            element.addClassName("dataGridFirstFooterCell");
        }

        @Override
        public void addLastCellStyle(TableCellElement element) {
            element.addClassName("dataGridLastFooterCell");
        }

        @Override
        public boolean isFooter() {
            return true;
        }
    }

    /**
     * Create a new DefaultHeaderBuilder for the header of footer section.
     *
     * @param table    the table being built
     * @param isFooter true if building the footer, false if the header
     */
    public DefaultHeaderBuilder(DataGrid<T> table, boolean isFooter) {
        super(table, isFooter ? new FooterDelegateImpl(table) : new HeaderDelegateImpl(table));
    }

    @Override
    protected void buildHeaderImpl(TableRowElement tr) {
        DataGrid<T> table = getTable();

        int columnCount = table.getColumnCount();
        for (int curColumn = 0; curColumn < columnCount; curColumn++) {
            Header<?> header = getHeader(curColumn);

            TableCellElement th = Document.get().createTHElement();
            delegate.setCellStyle(th);
            if (curColumn == 0)
                delegate.addFirstCellStyle(th);
            if (curColumn == columnCount - 1)
                delegate.addLastCellStyle(th);

            if(header != null)
                renderHeader(th, header);

            tr.appendChild(th);

            if(table.isColumnFlex(curColumn))
                th.setColSpan(2);
        }
    }

    @Override
    protected void updateHeaderImpl(TableRowElement tr) {
        DataGrid<T> table = getTable();

        // Early exit if there aren't any columns to render.
        int columnCount = table.getColumnCount();

        // Loop through all column headers.
        int curColumn;
        for (curColumn = 0; curColumn < columnCount; curColumn++) {
            Header<?> header = getHeader(curColumn);

            TableCellElement th = tr.getCells().getItem(curColumn).cast();

            updateHeader(th, header);
        }
    }

    @Override
    protected void updateHeaderStickyLeftImpl(TableRowElement tr, List<Integer> stickyColumns, List<DataGrid.StickyParams> stickyLefts) {
        GPropertyTableBuilder.updateStickyLeft(tr, stickyColumns, stickyLefts, true);
    }

    @Override
    protected void updateStickedState(TableRowElement tr, List<Integer> stickyColumns, int lastSticked) {
        GPropertyTableBuilder.updateStickyCellsClasses(tr, stickyColumns, lastSticked);
    }
}
