package lsfusion.client.form.controller;


import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockableRepository {
    /**
     * the formsList in this repository
     */
    private List<String> formsList = new ArrayList<>();

    /**
     * Writes the formsList of this repository into <code>out</code>.
     * @param out the stream to write into
     * @throws IOException if an I/O error occurs
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(formsList.size());
        for (String formID : formsList) {
            SerializationUtil.writeString(out, formID);
        }
    }


    /**
     * Reads the formsList of this repository from <code>in</code>.
     * @param in the stream to read from
     * @throws IOException if an I/O error occurs
     */
    public void read(DataInputStream in) throws IOException {
        formsList.clear();
        int n = in.readInt();
        for (int i = 0; i < n; ++i) {
            formsList.add(SerializationUtil.readString(in));
        }
    }

    /**
     * Adds a formID to the list of formsList.
     * @param formID the new formID
     */
    public void add(String formID) {
        formsList.add(formID);
    }

    /**
     * Removes a formID from the list of formsList.
     * @param formID the formID to remove
     */
    public void remove(String formID) {
        formsList.remove(formID);
    }

    public void clear() {
        formsList.clear();
    }

    public List<String> getFormsList() {
        return new ArrayList<>(formsList);
    }
}
