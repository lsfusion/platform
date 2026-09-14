package lsfusion.interop.form;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// the form goes into a window that holds forms - the one SHOW ... WINDOW named, or System.forms, whose name is put
// there when the open names none. The kind is a class rather than a constant of the enum beside it because a
// constant cannot carry the destination, and the destination is part of what opening into a window means
public class DockedWindowFormType implements WindowFormType {

    public final String window; // the canonical name of the FORMS window, never null

    public DockedWindowFormType(String window) {
        this.window = window;
    }

    // the byte the DOCKED constant used to have. It is not only a tag: AsyncMapOpenForm.merge picks between two
    // pre-opened types by comparing it, so moving this kind would silently change which branch wins a merge
    @Override
    public byte getType() {
        return 2;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        serializeType(outStream);
        outStream.writeUTF(window);
    }

    public static DockedWindowFormType deserialize(DataInputStream inStream) throws IOException {
        return new DockedWindowFormType(inStream.readUTF());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DockedWindowFormType && window.equals(((DockedWindowFormType) o).window);
    }

    @Override
    public int hashCode() {
        return window.hashCode();
    }

    @Override
    public String toString() {
        return "DOCKED " + window;
    }
}
