package platform.gwt.cellview.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;

class DataGridImpl {
    /**
     * Interface that this class's subclass may implement to get notified with table section change
     * event. During rendering, a faster method based on swaping the entire section will be used iff
     * <li> it's in IE - since all other optimizations have been turned off
     * <li> the table implements TableSectionChangeHandler interface
     * When a section is being replaced by another table with the new table html, the methods in this
     * interface will be invoked with the changed section. The table should update its internal
     * references to the sections properly so that when {@link #getTableBodyElement},
     * {@link #getTableHeadElement}, or {@link #getTableFootElement} are called, the correct section
     * will be returned.
     */
    protected interface TableSectionChangeHandler {
        /**
         * Notify that a table body section has been changed.
         *
         * @param newTBody the new body section
         */
        void onTableBodyChange(TableSectionElement newTBody);

        /**
         * Notify that a table body section has been changed.
         *
         * @param newTFoot the new foot section
         */
        void onTableFootChange(TableSectionElement newTFoot);

        /**
         * Notify that a table head section has been changed.
         *
         * @param newTHead the new head section
         */
        void onTableHeadChange(TableSectionElement newTHead);
    }

    interface Template extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<div style=\"outline:none;\">{0}</div>")
        SafeHtml div(SafeHtml contents);

        @SafeHtmlTemplates.Template("<table><tbody>{0}</tbody></table>")
        SafeHtml tbody(SafeHtml rowHtml);

        @SafeHtmlTemplates.Template("<td class=\"{0}\">{1}</td>")
        SafeHtml td(String classes, SafeHtml contents);

        @SafeHtmlTemplates.Template("<td class=\"{0}\" align=\"{1}\" valign=\"{2}\">{3}</td>")
        SafeHtml tdBothAlign(String classes, String hAlign, String vAlign, SafeHtml contents);

        @SafeHtmlTemplates.Template("<td class=\"{0}\" align=\"{1}\">{2}</td>")
        SafeHtml tdHorizontalAlign(String classes, String hAlign, SafeHtml contents);

        @SafeHtmlTemplates.Template("<td class=\"{0}\" valign=\"{1}\">{2}</td>")
        SafeHtml tdVerticalAlign(String classes, String vAlign, SafeHtml contents);

        @SafeHtmlTemplates.Template("<table><tfoot>{0}</tfoot></table>")
        SafeHtml tfoot(SafeHtml rowHtml);

        @SafeHtmlTemplates.Template("<table><thead>{0}</thead></table>")
        SafeHtml thead(SafeHtml rowHtml);

        @SafeHtmlTemplates.Template("<tr onclick=\"\" class=\"{0}\">{1}</tr>")
        SafeHtml tr(String classes, SafeHtml contents);
    }

    private static Template template;

    public static Template getTemplate() {
        if (template == null) {
            template = GWT.create(Template.class);
        }
        return template;
    }

    private final com.google.gwt.user.client.Element tmpElem = Document.get().createDivElement().cast();

    /**
     * Convert the rowHtml into Elements wrapped by the specified table section.
     *
     * @param table      the {@link DataGrid}
     * @param sectionTag the table section tag
     * @param rowHtml    the Html for the rows
     * @return the section element
     */
    public TableSectionElement convertToSectionElement(DataGrid<?> table,
                                                       String sectionTag, SafeHtml rowHtml) {
        // Attach an event listener so we can catch synchronous load events from
        // cached images.
        DOM.setEventListener(tmpElem, table);

      /*
       * Render the rows into a table.
       *
       * IE doesn't support innerHtml on a TableSection or Table element, so we
       * generate the entire table. We do the same for all browsers to avoid any
       * future bugs, since setting innerHTML on a table section seems brittle.
       */
        sectionTag = sectionTag.toLowerCase();
        if ("tbody".equals(sectionTag)) {
            tmpElem.setInnerSafeHtml(getTemplate().tbody(rowHtml));
        } else if ("thead".equals(sectionTag)) {
            tmpElem.setInnerSafeHtml(getTemplate().thead(rowHtml));
        } else if ("tfoot".equals(sectionTag)) {
            tmpElem.setInnerSafeHtml(getTemplate().tfoot(rowHtml));
        } else {
            throw new IllegalArgumentException("Invalid table section tag: " + sectionTag);
        }
        TableElement tableElem = tmpElem.getFirstChildElement().cast();

        // Detach the event listener.
        DOM.setEventListener(tmpElem, null);

        // Get the section out of the table.
        if ("tbody".equals(sectionTag)) {
            return tableElem.getTBodies().getItem(0);
        } else if ("thead".equals(sectionTag)) {
            return tableElem.getTHead();
        } else if ("tfoot".equals(sectionTag)) {
            return tableElem.getTFoot();
        } else {
            throw new IllegalArgumentException("Invalid table section tag: " + sectionTag);
        }
    }

    /**
     * Render a table section in the table.
     *
     * @param table   the {@link DataGrid}
     * @param section the {@link TableSectionElement} to replace
     * @param html    the html of a table section element containing the rows
     */
    public final void replaceAllRows(DataGrid<?> table, TableSectionElement section,
                                     SafeHtml html) {
        // If the widget is not attached, attach an event listener so we can catch
        // synchronous load events from cached images.
        if (!table.isAttached()) {
            DOM.setEventListener(table.getElement(), table);
        }

        // Remove the section from the tbody.
        Element parent = section.getParentElement();
        Element nextSection = section.getNextSiblingElement();
        detachSectionElement(section);

        // Render the html.
        replaceAllRowsImpl(table, section, html);

      /*
       * Reattach the section. If next section is null, the section will be
       * appended instead.
       */
        reattachSectionElement(parent, section, nextSection);

        // Detach the event listener.
        if (!table.isAttached()) {
            DOM.setEventListener(table.getElement(), null);
        }
    }

    /**
     * Replace a set of row values with newly rendered values.
     * <p/>
     * This method does not necessarily perform a one to one replacement. Some
     * row values may be rendered as multiple row elements, while others are
     * rendered as only one row element.
     *
     * @param table      the {@link DataGrid}
     * @param section    the {@link TableSectionElement} to replace
     * @param html       the html of a table section element containing the rows
     * @param startIndex the start index to replace
     * @param childCount the number of row values to replace
     */
    public final void replaceChildren(DataGrid<?> table, TableSectionElement section,
                                      SafeHtml html, int startIndex, int childCount) {
        // If the widget is not attached, attach an event listener so we can catch
        // synchronous load events from cached images.
        if (!table.isAttached()) {
            DOM.setEventListener(table.getElement(), table);
        }

        // Remove the section from the tbody.
        Element parent = section.getParentElement();
        Element nextSection = section.getNextSiblingElement();
        detachSectionElement(section);

        // Remove all children in the range.
        final int endIndex = startIndex + childCount;

        TableRowElement insertBefore = table.getChildElement(startIndex).cast();
        while (insertBefore != null && table.getTableBuilder().getRowValueIndex(insertBefore) < endIndex) {
            Element next = insertBefore.getNextSiblingElement();
            section.removeChild(insertBefore);
            insertBefore = (next == null) ? null : next.<TableRowElement>cast();
        }

        // Add new child elements.
        TableSectionElement newSection = convertToSectionElement(table, section.getTagName(), html);
        Element newChild = newSection.getFirstChildElement();
        while (newChild != null) {
            Element next = newChild.getNextSiblingElement();
            section.insertBefore(newChild, insertBefore);
            newChild = next;
        }

          /*
           * Reattach the section. If next section is null, the section will be
           * appended instead.
           */
        reattachSectionElement(parent, section, nextSection);

        // Detach the event listener.
        if (!table.isAttached()) {
            DOM.setEventListener(table.getElement(), null);
        }
    }

    /**
     * Detach a table section element from its parent.
     *
     * @param section the element to detach
     */
    protected void detachSectionElement(TableSectionElement section) {
        section.removeFromParent();
    }

    /**
     * Reattach a table section element from its parent.
     *
     * @param parent      the parent element
     * @param section     the element to reattach
     * @param nextSection the next section
     */
    protected void reattachSectionElement(Element parent, TableSectionElement section,
                                          Element nextSection) {
        parent.insertBefore(section, nextSection);
    }

    /**
     * Render a table section in the table.
     *
     * @param table   the {@link DataGrid}
     * @param section the {@link TableSectionElement} to replace
     * @param html    the html of a table section element containing the rows
     */
    protected void replaceAllRowsImpl(DataGrid<?> table, TableSectionElement section,
                                      SafeHtml html) {
        section.setInnerSafeHtml(html);
    }
}

