package platform.server.logics.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.data.KeyField;
import platform.server.data.types.Type;
import platform.server.logics.data.TableFactory;
import platform.server.session.DataSession;
import platform.server.view.form.client.report.ReportDrawField;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;
import java.util.*;

public class ObjectClass extends RemoteClass {

    public ObjectClass(Integer iID, String caption, RemoteClass... parents) {super(iID, caption, parents); }

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        ArrayList<Map<KeyField,Integer>> Result = new ArrayList<Map<KeyField,Integer>>(tableFactory.objectTable.getClassJoin(this).executeSelect(session).keySet());
        return Result.get(randomizer.nextInt(Result.size())).get(tableFactory.objectTable.key);
    }

    public Object getRandomObject(Map<RemoteClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
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

        for(RemoteClass parent : parents)
            parent.fillParents(parentSet);
    }

    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

    public byte getTypeID() {
        return 0;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(!childs.isEmpty());
    }
}
