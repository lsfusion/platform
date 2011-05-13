package skolkovo.gwt.base.client.ui;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import skolkovo.gwt.base.client.BaseMessages;

public abstract class YesNoColumn<T> extends Column<T, Boolean> {
    private static class YesNoCell extends AbstractCell<Boolean> {
        @Override
        public void render(Boolean value, Object key, SafeHtmlBuilder sb) {
            BaseMessages baseMessages = BaseMessages.Instance.get();
            sb.append(SafeHtmlUtils.fromString(value != null && value ? baseMessages.yes() : baseMessages.no()));
        }
    }

    public YesNoColumn() {
        super(new YesNoCell());
    }
}
