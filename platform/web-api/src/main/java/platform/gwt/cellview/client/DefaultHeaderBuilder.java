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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;

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
        public String getHeaderStyle() {
            return grid.getResources().style().dataGridHeader();
        }

        @Override
        public String getFirstColumnStyle() {
            return grid.getResources().style().dataGridFirstColumnHeader();
        }

        @Override
        public String getLastColumnStyle() {
            return grid.getResources().style().dataGridLastColumnHeader();
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
        public String getHeaderStyle() {
            return grid.getResources().style().dataGridFooter();
        }

        @Override
        public String getFirstColumnStyle() {
            return grid.getResources().style().dataGridFirstColumnFooter();
        }

        @Override
        public String getLastColumnStyle() {
            return grid.getResources().style().dataGridLastColumnFooter();
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

        // Early exit if there aren't any columns to render.
        int columnCount = table.getColumnCount();

        // Get the common style names.
        String headerClass = delegate.getHeaderStyle();
        String firstColumnClass = delegate.getFirstColumnStyle();
        String lastColumnClass = delegate.getLastColumnStyle();

        // Loop through all column headers.
        int curColumn;
        for (curColumn = 0; curColumn < columnCount; curColumn++) {
            Header<?> header = getHeader(curColumn);

            TableCellElement th = tr.appendChild(Document.get().createTHElement());

            //куча if-ов, чтобы не заморачиваться с StringBuilder
            if (curColumn == 0 && curColumn == columnCount - 1) {
                th.setClassName(headerClass + " " + firstColumnClass + " " + lastColumnClass);
            } else if (curColumn == 0) {
                th.setClassName(headerClass + " " + firstColumnClass);
            } else if (curColumn == columnCount - 1) {
                th.setClassName(headerClass + " " + lastColumnClass);
            } else {
                th.setClassName(headerClass);
            }

            renderHeader(th, header);
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
}
