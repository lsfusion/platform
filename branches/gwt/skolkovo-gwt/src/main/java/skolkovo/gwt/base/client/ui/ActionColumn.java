package skolkovo.gwt.base.client.ui;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;

public abstract class ActionColumn<T> extends Column<T, T> {
    private final String defaultCaption;

    public static class ActionCell<C> extends AbstractCell<C> {
        private ActionColumn<C> owner;

        public ActionCell() {
            super("click", "keydown");
        }

        @Override
        public void onBrowserEvent(Element parent, C value, Object key,
                                   NativeEvent event, ValueUpdater<C> valueUpdater) {
            super.onBrowserEvent(parent, value, key, event, valueUpdater);
            if ("click".equals(event.getType())) {
                onEnterKeyDown(parent, value, key, event, valueUpdater);
            }
        }

        @Override
        public void render(C value, Object key, SafeHtmlBuilder sb) {
            if (!owner.hidden(value)) {
                SafeHtml html = new SafeHtmlBuilder()
                        .appendHtmlConstant("<button type=\"button\" tabindex=\"-1\">")
                        .append(SafeHtmlUtils.fromString(owner.getCaption(value)))
                        .appendHtmlConstant("</button>")
                        .toSafeHtml();
                sb.append(html);
            }
        }

        @Override
        protected void onEnterKeyDown(Element parent, C value, Object key,
                                      NativeEvent event, ValueUpdater<C> valueUpdater) {
            if (!owner.hidden(value)) {
                owner.execute(value);
            }
        }

        public void setOwner(ActionColumn<C> owner) {
            this.owner = owner;
        }
    }

    public ActionColumn(String defaultCaption) {
        super(new ActionCell<T>());
        ((ActionCell<T>)cell).setOwner(this);
        this.defaultCaption = defaultCaption;
    }

    @Override
    public T getValue(T object) {
        return object;
    }

    public abstract void execute(T object);

    public String getCaption(T object) {
        return defaultCaption;
    }

    public boolean  hidden(T object) {
        return false;
    }
}
