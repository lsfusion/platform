package lsfusion.gwt.client.base.view.grid;

public interface GridStyle {
    /**
     * Applied to the table.
     */
    String dataGridWidget();

    /**
     * applied to the whole header
     */
    String dataGridHeader();

    /**
     * Applied to headers cells.
     */
    String dataGridHeaderCell();

    /**
     * Applied to the first column headers.
     */
    String dataGridFirstHeaderCell();

    /**
     * Applied to the last column headers.
     */
    String dataGridLastHeaderCell();

    /**
     * applied to the whole footer
     */
    String dataGridFooter();

    /**
     * Applied to footers cells.
     */
    String dataGridFooterCell();

    /**
     * Applied to the first column footers.
     */
    String dataGridFirstFooterCell();

    /**
     * Applied to the last column footers.
     */
    String dataGridLastFooterCell();


    /**
     * Applied to rows.
     */
    String dataGridRow();

    /**
     * Applied to cell.
     */
    String dataGridCell();

    /**
     * Applied to the first column.
     */
    String dataGridFirstCell();

    /**
     * Applied to the last column.
     */
    String dataGridLastCell();
}
