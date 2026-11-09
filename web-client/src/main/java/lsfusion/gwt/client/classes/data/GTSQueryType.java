package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;

public class GTSQueryType extends GTextSearchType {
    public static GTSQueryType instance = new GTSQueryType();

    // a query is a few words, not a document's lexemes: one line, as wide as the desktop client draws it
    @Override
    protected int getDefaultCharWidth() {
        return 15;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTSQueryCaption();
    }
}
