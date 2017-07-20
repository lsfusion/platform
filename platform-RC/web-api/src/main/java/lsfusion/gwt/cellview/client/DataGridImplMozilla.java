package lsfusion.gwt.cellview.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableSectionElement;

/**
 * Implementation of {@link CellTable} used by Firefox.
 */
@SuppressWarnings("unused")
class DataGridImplMozilla extends DataGridImpl {
    /**
     * Firefox 3.6 and earlier convert td elements to divs if the tbody is
     * removed from the table element.
     */
    @Override
    protected void detachSectionElement(TableSectionElement section) {
        if (isGecko192OrBefore()) {
            return;
        }
        super.detachSectionElement(section);
    }

    @Override
    protected void reattachSectionElement(Element parent, TableSectionElement section,
                                          Element nextSection) {
        if (isGecko192OrBefore()) {
            return;
        }
        super.reattachSectionElement(parent, section, nextSection);
    }

    /**
     * Return true if using Gecko 1.9.2 (Firefox 3.6) or earlier.
     */
    private native boolean isGecko192OrBefore() /*-{
        return @com.google.gwt.dom.client.DOMImplMozilla::isGecko192OrBefore()();
    }-*/;
}
