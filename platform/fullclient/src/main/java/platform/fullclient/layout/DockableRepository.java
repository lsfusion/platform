package platform.fullclient.layout;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DockableRepository {
    /**
     * the formsList in this repository
     */
    private List<String> formsList = new ArrayList<String>();

    /**
     * Writes the formsList of this repository into <code>out</code>.
     *
     * @param out the stream to write into
     * @throws IOException if an I/O error occurs
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(formsList.size());
        for (String formID : formsList) {
            out.writeUTF(formID);
        }
    }


    /**
     * Reads the formsList of this repository from <code>in</code>.
     *
     * @param in the stream to read from
     * @throws IOException if an I/O error occurs
     */
    public void read(DataInputStream in) throws IOException {
        formsList.clear();
        int n = in.readInt();
        for (int i = 0; i < n; ++i) {
            formsList.add(in.readUTF());
        }
    }

    /**
     * Adds a formID to the list of formsList.
     *
     * @param formID the new formID
     */

    public void add(String formID) {
        formsList.add(formID);
    }

    /**
     * Removes a formID from the list of formsList.
     *
     * @param formID the formID to remove
     */
    public void remove(String formID) {
        formsList.remove(formID);
    }

    public List<String> getFormsList() {
        return formsList;
    }
}
