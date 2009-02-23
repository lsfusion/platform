package platform.server.logics.classes;

import java.util.Random;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

import platform.server.data.types.Type;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;

public class StringClass extends DataClass {

    StringClass(Integer iID, String caption, int iLength) {
        super(iID, caption);
        length = iLength;
    }

    int length;

    public Type getType() {
        return Type.string(length);
    }

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return "NAME "+ randomizer.nextInt(50);
    }

    public Object getRandomObject(Map<DataClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return "NAME "+ randomizer.nextInt(diap);
    }

}
