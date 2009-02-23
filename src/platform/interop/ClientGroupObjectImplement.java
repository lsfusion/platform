package platform.interop;

import java.util.ArrayList;
import java.io.Serializable;

public class ClientGroupObjectImplement extends ArrayList<ClientObjectImplement>
                                 implements Serializable {

    public Integer ID = 0;

    public Boolean singleViewType = false;

    public ClientGridView gridView = new ClientGridView();
    public ClientFunctionView addView = new ClientFunctionView();
    public ClientFunctionView changeClassView = new ClientFunctionView();
    public ClientFunctionView delView = new ClientFunctionView();

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this.ID != null ? this.ID.hashCode() : 0);
        return hash;
    }

    public ClientGroupObjectImplement() {
    }
}
