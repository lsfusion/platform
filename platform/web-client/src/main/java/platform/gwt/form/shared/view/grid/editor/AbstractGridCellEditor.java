package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import platform.gwt.cellview.client.cell.Cell;

public abstract class AbstractGridCellEditor implements GridCellEditor {

    @Override
    public void renderDom(Cell.Context context, DivElement cellParent, Object value) {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        render(context, value, sb);
        cellParent.setInnerSafeHtml(sb.toSafeHtml());
    }

    protected void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        //do nothing by default
    }
}
