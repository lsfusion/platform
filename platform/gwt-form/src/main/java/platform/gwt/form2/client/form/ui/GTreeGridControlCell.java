package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;

public class GTreeGridControlCell extends AbstractCell<Object> {
    private GTreeTable treeTable;

    interface Template extends SafeHtmlTemplates {
        @Template("<div style=\"float: left; height: 16px;\"><img id=\"{0}\" src=\"{1}\"/></div>")
        SafeHtml node(String id, SafeUri url);

        @Template("<div style=\"float: left; height: 16px;\"><img src=\"{0}\"/></div>")
        SafeHtml dots(SafeUri url);
    }

    private static Template template;

    public GTreeGridControlCell(GTreeTable table) {
        super("click", "dblclick");
        treeTable = table;

        if (template == null) {
            template = GWT.create(Template.class);
        }
    }


    @Override
    public void onBrowserEvent(Context context, Element parent, Object value, NativeEvent event, ValueUpdater<Object> valueUpdater) {
        if ("dblclick".equals(event.getType())) {
            changeTreeState(context, value, event);
        } else if ("click".equals(event.getType())) {
            if (event.getEventTarget().equals(DOM.getElementById("treeButton" + ((GTreeColumnValue) value).getSID()))) {
                changeTreeState(context, value, event);
            }
        } else {
            super.onBrowserEvent(context, parent, value, event, valueUpdater);
        }
    }

    private void changeTreeState(Context context, Object value, NativeEvent event) {
        event.preventDefault();
        Boolean open = ((GTreeColumnValue) value).getOpen();
        if (open != null) {
            if (!open) {
                treeTable.fireExpandNode((GTreeGridRecord) context.getKey());
            } else {
                treeTable.fireCollapseNode((GTreeGridRecord) context.getKey());
            }
        }
    }

    @Override
    public void render(Context context, Object value, SafeHtmlBuilder sb) {
        GTreeColumnValue treeValue = (GTreeColumnValue) value;
        String iconName;
        if (treeValue.getOpen() == null) {
            iconName = "tree_leaf.png";
        } else {
            iconName = treeValue.getOpen() ? "tree_open.png" : "tree_closed.png";
        }
        for (int i = 0; i < treeValue.getLevel() - 1; i++) {
            if (!treeValue.isLastInLevel(i)) {
                sb.append(template.dots(getImageURI("tree_dots_passby.png")));
            } else {
                sb.append(template.dots(getImageURI("tree_empty.png")));
            }
        }
        if (treeValue.getLevel() > 0) {
            if (!treeValue.isLastInLevel(treeValue.getLevel() - 1)) {
                sb.append(template.dots(getImageURI("tree_dots_branch.png")));
            } else {
                sb.append(template.dots(getImageURI("tree_dots_end.png")));
            }
        }
        sb.append(template.node("treeButton" + treeValue.getSID(), getImageURI(iconName)));
    }

    private SafeUri getImageURI(String imageName) {
        return UriUtils.fromString(GWT.getModuleBaseURL() + "images/" + imageName);
    }
}
