package platform.server.logics.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.data.types.Type;
import platform.server.logics.data.TableFactory;
import platform.server.session.DataSession;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;

// класс который можно сравнивать
public class IntegralClass extends RemoteClass {

    IntegralClass(Integer iID, String caption) {super(iID, caption);}

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return randomizer.nextInt(diap * diap +1);
    }

    public Object getRandomObject(Map<RemoteClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return randomizer.nextInt(diap);
    }

    public Type getType() {
        return Type.integer;
    }

    public int getMinimumWidth() { return 45; }
    public int getPreferredWidth() { return 80; }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

    public Class getJavaClass() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte getTypeID() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
