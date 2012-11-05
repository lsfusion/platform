package platform.gwt.form.client.form.ui;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import platform.gwt.cellview.client.Header;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GGridHeaderCell extends TextCell {
    private Header header;
    private GGridPropertyTable table;

    interface Template extends SafeHtmlTemplates {
        @Template("<div title=\"{2}\">" +
                    "<img style=\"float:left; height:15px; width: 15px;\" src=\"{1}\"/>" +
                    "<span style=\"white-space:normal;\">{0}</span>" +
                  "</div>"
        )
        SafeHtml arrow(SafeHtml caption, SafeUri url, String title);

        @Template("<div style=\"white-space:normal;\" title=\"{1}\">{0}</div>")
        SafeHtml textWithTooltip(SafeHtml caption, String title);
    }

    private static Template template;

    public GGridHeaderCell() {
        super();
        if (template == null) {
            template = GWT.create(Template.class);
        }
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public void setTable(GGridPropertyTable table) {
        this.table = table;
    }

    @Override
    public Set<String> getConsumedEvents() {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"dblclick"})));
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event, ValueUpdater<String> valueUpdater) {
        if ("dblclick".equals(event.getType())) {
            table.headerClicked(header, event.getCtrlKey());
        } else {
            super.onBrowserEvent(context, parent, value, event, valueUpdater);
        }
    }

    @Override
    public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
        Boolean sortDir = table.getSortDirection(header);
        if (sortDir != null) {
            sb.append(template.arrow(value, UriUtils.fromString(GWT.getModuleBaseURL() + "images/" + (sortDir ? "arrowup.png" : "arrowdown.png")), value.asString()));
        } else {
            sb.append(template.textWithTooltip(value, value.asString()));
        }
    }
}
