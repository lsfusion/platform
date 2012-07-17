package platform.gwt.form.server;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JasperExportManager;
import platform.base.BaseUtils;
import platform.client.logics.ClientFormChanges;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.view.actions.*;
import platform.gwt.view.classes.GObjectClass;
import platform.interop.action.*;
import platform.interop.form.ReportGenerationData;

import javax.servlet.http.HttpSession;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

@SuppressWarnings("UnusedDeclaration")
public class ClientToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientToGwtConverter instance = new ClientToGwtConverter();
    }

    public static ClientToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private ClientToGwtConverter() {
    }

    public GAction convertAction(ClientAction clientAction, Object... context) {
        return convertOrNull(clientAction, context);
    }

    @Converter(from = Color.class)
    public String convertColor(Color color) {
        return String.format("#%06X", (0xFFFFFF & color.getRGB()));
    }

    @Converter(from = ChooseClassClientAction.class)
    public GChooseClassAction convertAction(ChooseClassClientAction action) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(action.classes));
        ClientObjectClass baseClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream);
        ClientObjectClass defaultClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream);

        return new GChooseClassAction(action.concrete, convertObjectClass(baseClass), convertObjectClass(defaultClass));
    }

    @Converter(from = ConfirmClientAction.class)
    public GConfirmAction convertAction(ConfirmClientAction action) {
        return new GConfirmAction(action.message, action.caption);
    }

    @Converter(from = DialogClientAction.class)
    public GDialogAction convertAction(DialogClientAction action, LogicsDispatchServlet servlet) throws IOException {
//        return new GDialogAction(servlet.getFormSessionManager().createFormAndPutInSession(action.dialog));
        return null;
    }

    @Converter(from = FormClientAction.class)
    public GFormAction convertAction(FormClientAction action, LogicsDispatchServlet servlet) throws IOException {
//        return new GFormAction(action.isModal, servlet.getFormSessionManager().createFormAndPutInSession(action.remoteForm));
        return null;
    }

    @Converter(from = HideFormClientAction.class)
    public GHideFormAction convertAction(HideFormClientAction action, LogicsDispatchServlet servlet) throws IOException {
        return new GHideFormAction();
    }

    @Converter(from = LogMessageClientAction.class)
    public GLogMessageAction convertAction(LogMessageClientAction action, LogicsDispatchServlet servlet) throws IOException {
        return new GLogMessageAction(action.failed, action.message);
    }

    @Converter(from = MessageClientAction.class)
    public GMessageAction convertAction(MessageClientAction action) {
        return new GMessageAction(action.message, action.caption);
    }

    @Converter(from = ProcessFormChangesClientAction.class)
    public GProcessFormChangesAction convertAction(ProcessFormChangesClientAction action, FormSessionObject form) throws IOException {
        ClientFormChanges changes = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(action.formChanges)), form.clientForm);
//        return new GProcessFormChangesAction(changes.getGwtFormChangesDTO());
        return null;
    }

    @Converter(from = ReportClientAction.class)
    public GReportAction convertAction(ReportClientAction action, LogicsDispatchServlet servlet) throws IOException {
        //todo:
        return new GReportAction();
    }

    @Converter(from = RequestUserInputClientAction.class)
    public GRequestUserInputAction convertAction(RequestUserInputClientAction action) throws IOException {
//        return new GRequestUserInputAction(ClientTypeSerializer.deserializeClientType(action.readType).getGwtType(), action.oldValue);
        return null;
    }

    @Converter(from = RunPrintReportClientAction.class)
    public GRunPrintReportAction convertAction(RunPrintReportClientAction action, HttpSession session, FormSessionObject form) throws IOException {
        return new GRunPrintReportAction(generateReport(session, form, true));
    }

    @Converter(from = RunOpenInExcelClientAction.class)
    public GRunOpenInExcelAction convertAction(RunOpenInExcelClientAction action, HttpSession session, FormSessionObject form) throws IOException {
        return new GRunOpenInExcelAction(generateReport(session, form, false));
    }

    private String generateReport(HttpSession session, FormSessionObject form, boolean isPdf) {
        try {
            TimeZone zone = Calendar.getInstance().getTimeZone();
            ReportGenerationData data = form.remoteForm.getReportData(-1, null, !isPdf, null);
            ReportGenerator generator = new ReportGenerator(data, zone);
            byte[] report = isPdf ? JasperExportManager.exportReportToPdf(generator.createReport(false, null)) : ReportGenerator.exportToExcelByteArray(data, zone);
            File file = File.createTempFile("lsfReport", "");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(report);

            String reportSID = generateReportSID(session);
            session.setAttribute(reportSID, file.getAbsolutePath());
            return reportSID;
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        String s = BaseUtils.lineSeparator;
        return null;
    }

    private String generateReportSID(HttpSession session) {
        String sid = "";
        do {
            sid = BaseUtils.randomString(20);
        } while (session.getAttribute(sid) != null);
        return sid;
    }

    @Converter(from = ClientObjectClass.class)
    public GObjectClass convertObjectClass(ClientObjectClass clientClass) {
        ArrayList<GObjectClass> children = new ArrayList<GObjectClass>();
        for (ClientObjectClass child : clientClass.getChildren()) {
            children.add(convertObjectClass(child));
        }

        return new GObjectClass(clientClass.getID(), clientClass.isConcreate(), clientClass.getCaption(), children);
    }
}
