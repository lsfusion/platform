package platform.server.logics.classes;

import java.util.Random;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;
import java.text.Format;

import platform.server.data.types.Type;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;
import platform.server.view.form.report.ReportDrawField;
import net.sf.jasperreports.engine.JRAlignment;

public class BitClass extends IntegralClass {
    BitClass(Integer iID, String caption) {super(iID, caption);}

    public Type getType() {
        return Type.bit;
    }

    public Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return randomizer.nextBoolean();
    }

    public Object getRandomObject(Map<RemoteClass, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return randomizer.nextBoolean();
    }

    public int getPreferredWidth() { return 35; }

    public Format getDefaultFormat() {
        return null;
    }

    public Class getJavaClass() {
        return Boolean.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_CENTER;
    }

    public byte getTypeID() {
        return 4;
    }
}
