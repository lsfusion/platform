package platform.server.logics.classes;

import java.util.*;
import java.sql.SQLException;

import platform.server.logics.session.DataSession;
import platform.server.logics.data.TableFactory;
import platform.server.data.types.Type;
import platform.server.data.KeyField;

public class ObjectClass extends DataClass {

    public ObjectClass(Integer iID, String caption, DataClass... parents) {super(iID, caption, parents); }

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        ArrayList<Map<KeyField,Integer>> Result = new ArrayList<Map<KeyField,Integer>>(tableFactory.objectTable.getClassJoin(this).executeSelect(session).keySet());
        return Result.get(randomizer.nextInt(Result.size())).get(tableFactory.objectTable.key);
    }

    public Object getRandomObject(Map<DataClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        List<Integer> classObjects = objects.get(this);
        return classObjects.get(randomizer.nextInt(classObjects.size()));
    }

    public Type getType() {
        return Type.object;
    }

    public void fillParents(Collection<ObjectClass> parentSet) {
        if (parentSet.contains(this))
            return;

        parentSet.add(this);

        for(DataClass parent : parents)
            parent.fillParents(parentSet);
    }
}
