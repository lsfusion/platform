package lsfusion.client.form.controller;


import lsfusion.client.base.view.ClientDockable;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DockableRepository {

    private List<ClientDockable> formsList = new ArrayList<>();

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(0); //backward compatibility
    }

    public void read(DataInputStream in) throws IOException {
        formsList.clear();
        int n = in.readInt(); //backward compatibility
        for (int i = 0; i < n; ++i) {
            SerializationUtil.readString(in);
        }
    }

    public void add(ClientDockable form) {
        formsList.add(form);
    }

    public void remove(ClientDockable form) {
        formsList.remove(form);
    }

    public void clear() {
        formsList.clear();
    }

    public List<ClientDockable> getFormsList() {
        return formsList;
    }
}
