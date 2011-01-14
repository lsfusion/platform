package platform.fullclient.layout;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FormRepository {
    /** observers of this repository, will be informed whenever formsList are added or removed */
    private List<FormRepositoryListener> listeners = new ArrayList<FormRepositoryListener>();

    /** the formsList in this repository*/
    private List<Integer> formsList = new ArrayList<Integer>();

    /**
     * Writes the formsList of this repository into <code>out</code>.
     * @param out the stream to write into
     * @throws IOException if an I/O error occurs
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(formsList.size());
        for (Integer formID : formsList) {
            out.writeInt(formID);
        }
    }


    /**
     * Reads the formsList of this repository from <code>in</code>.
     * @param in the stream to read from
     * @throws IOException if an I/O error occurs
     */
    public void read(DataInputStream in) throws IOException {
        formsList.clear();

        for (int i = 0, n = in.readInt(); i < n; ++i) {
            add(in.readInt());
        }
    }

    /**
     * Adds a formID to the list of formsList.
     * @param formID the new formID
     */
    public void add(Integer formID) {
        if (formsList.add(formID)) {
            for (FormRepositoryListener listener : listeners) {
                listener.pictureAdded(formID);
            }
        }
    }

    /**
     * Removes a formID from the list of formsList.
     * @param formID the formID to remove
     */
    public void remove(Integer formID) {
        if (formsList.remove(formID)) {
            for (FormRepositoryListener listener : listeners) {
                listener.pictureRemoved(formID);
            }
        }
    }
}
