package skolkovo.gwt.base.client.ui;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;

public abstract class LinkColumn<T> extends Column<T, String> {
    private static class LinkCell extends AbstractCell<String> {
        @Override
        public void render(String value, Object key, SafeHtmlBuilder sb) {
            if (value == null) {
                return;
            }
            sb.appendHtmlConstant(value);
        }
    }

    public LinkColumn() {
        super(new LinkCell());
    }

    @Override
    public final String getValue(T object) {
        String linkUrl = getLinkUrl(object);
        String linkText = getLinkText(object);

        SafeHtml safeUrl = SafeHtmlUtils.fromString(linkUrl != null ? linkUrl : "");
        SafeHtml safeText = SafeHtmlUtils.fromString(linkText != null ? linkText : "");

        return "<a href=\"" + safeUrl.asString() + "\">" + safeText.asString() + "</>";
    }

    public abstract String getLinkUrl(T object);
    public abstract String getLinkText(T object);
}
