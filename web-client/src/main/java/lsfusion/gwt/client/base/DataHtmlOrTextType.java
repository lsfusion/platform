package lsfusion.gwt.client.base;

import lsfusion.gwt.client.view.MainFrame;

public class DataHtmlOrTextType extends HtmlOrTextType {

    public static final DataHtmlOrTextType TEXT = new DataHtmlOrTextType();
    public static final DataHtmlOrTextType TEXTBASED = new DataHtmlOrTextType();
    public static final DataHtmlOrTextType HTML = new DataHtmlOrTextType();

    @Override
    protected boolean isCollapse() {
        return this == TEXTBASED;
    }

    @Override
    protected boolean isWrap() {
        return MainFrame.contentWordWrap;
    }
}
