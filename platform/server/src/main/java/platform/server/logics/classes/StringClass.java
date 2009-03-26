package platform.server.logics.classes;

import platform.server.data.types.Type;
import platform.server.logics.data.TableFactory;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.text.Format;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.DataOutputStream;
import java.io.IOException;

public class StringClass extends RemoteClass {

    int length;

    StringClass(Integer iID, String caption, int iLength) {
        super(iID, caption);
        length = iLength;
    }

    public Type getType() {
        return Type.string(length);
    }

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return "NAME "+ randomizer.nextInt(50);
    }

    public Object getRandomObject(Map<RemoteClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return "NAME "+ randomizer.nextInt(diap);
    }

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 250; }

    public Format getDefaultFormat() {
        return null;
    }

    public Class getJavaClass() {
        return String.class;
    }

    public byte getTypeID() {
        return 7;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(length);
    }
}
