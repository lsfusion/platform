package platform.client.form;

import platform.client.logics.ClientRegularFilter;

public class ClientRegularFilterWrapper {
    public ClientRegularFilter filter;
    private String caption;

    public ClientRegularFilterWrapper(String caption) {
        this(caption, null);
    }

    public ClientRegularFilterWrapper(ClientRegularFilter filter) {
        this(null, filter);
    }

    public ClientRegularFilterWrapper(String caption, ClientRegularFilter filter) {
        this.filter = filter;
        this.caption = caption;
    }

    @Override
    public boolean equals(Object wrapped) {
        return wrapped instanceof ClientRegularFilterWrapper
                && (filter != null ? filter.equals(((ClientRegularFilterWrapper) wrapped).filter) : this == wrapped);
    }

    @Override
    public String toString() {
        return caption == null ? filter.getFullCaption() : caption;
    }
}
