package lsfusion.gwt.cellview.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Implementation of {@link lsfusion.gwt.cellview.client.DataGrid} used by IE.
 */
@SuppressWarnings("unused")
class DataGridImplTrident extends DataGridImpl {

    /**
     * A different optimization is used in IE.
     */
    @Override
    protected void detachSectionElement(TableSectionElement section) {
    }

    @Override
    protected void reattachSectionElement(Element parent, TableSectionElement section, Element nextSection) {
    }

    /**
     * Instead of replacing each TR element, swaping out the entire section is much faster. If
     * the table has a sectionChangeHandler, this method will be used.
     */
    @Override
    protected void replaceAllRowsImpl(DataGrid<?> table, TableSectionElement section,
                                      SafeHtml html) {
        if (table instanceof TableSectionChangeHandler) {
            replaceTableSection(table, section, html);
        } else {
            replaceAllRowsImplLegacy(table, section, html);
        }
    }

    /**
     * This method is used for legacy AbstractCellTable that's not a
     * {@link lsfusion.gwt.cellview.client.DataGridImpl.TableSectionChangeHandler}.
     */
    protected void replaceAllRowsImplLegacy(DataGrid<?> table, TableSectionElement section,
                                            SafeHtml html) {
        // Remove all children.
        Element child = section.getFirstChildElement();
        while (child != null) {
            Element next = child.getNextSiblingElement();
            section.removeChild(child);
            child = next;
        }

        // Add new child elements.
        TableSectionElement newSection = convertToSectionElement(table, section.getTagName(), html);
        child = newSection.getFirstChildElement();
        while (child != null) {
            Element next = child.getNextSiblingElement();
            section.appendChild(child);
            child = next;
        }
    }

    /**
     * Render html into a table section. This is achieved by first setting the html in a DIV
     * element, and then swap the table section with the corresponding element in the DIV. This
     * method is used in IE since the normal optimizations are not feasible.
     *
     * @param table   the {@link lsfusion.gwt.cellview.client.DataGrid}
     * @param section the {@link com.google.gwt.dom.client.TableSectionElement} to replace
     * @param html    the html of a table section element containing the rows
     */
    private void replaceTableSection(DataGrid<?> table, TableSectionElement section,
                                     SafeHtml html) {
        String sectionName = section.getTagName().toLowerCase();
        TableSectionElement newSection = convertToSectionElement(table, sectionName, html);
        TableElement tableElement = table.getElement().cast();
        tableElement.replaceChild(newSection, section);
        if ("tbody".equals(sectionName)) {
            ((TableSectionChangeHandler) table).onTableBodyChange(newSection);
        } else if ("thead".equals(sectionName)) {
            ((TableSectionChangeHandler) table).onTableHeadChange(newSection);
        } else if ("tfoot".equals(sectionName)) {
            ((TableSectionChangeHandler) table).onTableFootChange(newSection);
        }
    }
}
