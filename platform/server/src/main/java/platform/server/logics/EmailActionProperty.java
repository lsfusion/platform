package platform.server.logics;

import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.EmailSender;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.reportstmp.ReportGenerator_tmp;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyMapImplement;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 03.01.11
 * Time: 11:53
 */

public class EmailActionProperty extends ActionProperty {
    public static enum Format {PDF, DOCX, HTML}
    private static Map<Format, String> extensions = new HashMap<Format, String>();
    static {
        extensions.put(Format.PDF, ".pdf");
        extensions.put(Format.DOCX, ".docx");
        extensions.put(Format.HTML, ".html");
    }

    public static enum FormStorageType {TEXT, ATTACH}

    private final List<FormEntity> forms;
    private final List<Format> formats = new ArrayList<Format>();
    private final List<FormStorageType> types = new ArrayList<FormStorageType>();
    private final List<Map<ObjectEntity, ClassPropertyInterface>> mapObjects = new ArrayList<Map<ObjectEntity, ClassPropertyInterface>>();

    private final List<PropertyMapImplement<?, ClassPropertyInterface>> recepients = new ArrayList<PropertyMapImplement<?, ClassPropertyInterface>>();

    private final String subject;

    private final BusinessLogics<?> BL; // для возможности работы с формами в автоматическом режиме

    public EmailActionProperty(String sID, String caption, String mailSubject, BusinessLogics<?> BL, ValueClass[] classes) {
        super(sID, caption, classes);

        this.subject = mailSubject;
        forms = new ArrayList<FormEntity>();
        this.BL = BL;

        askConfirm = true;
    }

    public <R extends PropertyInterface> void addRecepient(PropertyMapImplement<R, ClassPropertyInterface> recepient) {
        recepients.add(recepient);
    }

    public void addForm(FormEntity form, Format format, FormStorageType type, Map<ObjectEntity, ClassPropertyInterface> objects) {
        assert type == FormStorageType.ATTACH || type == FormStorageType.TEXT && format == Format.HTML;
        forms.add(form);
        formats.add(format);
        types.add(type);
        mapObjects.add(objects);
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) {
        throw new RuntimeException("should not be");
    }

    private static JRAbstractExporter createExporter(Format format) {
        JRAbstractExporter exporter = null;
        switch (format) {
            case PDF: exporter = new JRPdfExporter(); break;
            case DOCX: exporter = new JRDocxExporter(); break;
            case HTML:
            {
                exporter = new JRHtmlExporter();
                exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, false);
                break;
            }
        }
        return exporter;
    }

    private String createReportFile(JasperPrint print, Format format) {
        print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");
        try {
            String filePath = File.createTempFile("lsfReport", extensions.get(format)).getAbsolutePath();
            JRAbstractExporter exporter = createExporter(format);

            exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, filePath);
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.exportReport();

            return filePath;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) throws SQLException {

        try {
            List<EmailSender.AttachmentProperties> reportPaths = new ArrayList<EmailSender.AttachmentProperties>();
            Map<ByteArray, String> files = new HashMap<ByteArray, String>();
            List<String> bodyFiles = new ArrayList<String>();
            for (int i = 0; i < forms.size(); i++) {
                Map<ObjectEntity, DataObject> formObjects = BaseUtils.join(mapObjects.get(i), keys);
                RemoteFormInterface remoteForm;
                if(executeForm!=null)
                    remoteForm = executeForm.createForm(forms.get(i), formObjects);
                else
                    remoteForm = BL.createForm(session, forms.get(i), formObjects);

                ReportGenerator_tmp report = new ReportGenerator_tmp(remoteForm, false, true, files);
                JasperPrint print = report.createReport();
                String filePath = createReportFile(print, formats.get(i));
                if (types.get(i) == FormStorageType.TEXT) {
                    bodyFiles.add(filePath);
                } else {
                    reportPaths.add(new EmailSender.AttachmentProperties(filePath, forms.get(i).caption, formats.get(i)));
                }
            }

            Modifier<?> modifier = executeForm!=null? executeForm.form :session.modifier;

            List<String> recepientEmails = new ArrayList<String>();
            for(PropertyMapImplement<?, ClassPropertyInterface> recepient : recepients) {
                String recepientEmail = (String) recepient.read(session, keys, modifier);
                if(recepientEmail!=null)
                    recepientEmails.add(recepientEmail);
            }

            String smtpHost = (String) BL.smtpHost.read(session);
            String smtpPort = (String) BL.smtpPort.read(session);
            String fromAddress = (String) BL.fromAddress.read(session);
            String userName = (String) BL.emailAccount.read(session);
            String password = (String) BL.emailPassword.read(session);
            if(smtpHost==null || fromAddress==null)
                actions.add(new MessageClientAction("Не задан SMTP хост или адрес отправителя. Письма отосланы не будут.","Отсылка писем"));
            else {
                try {
                    EmailSender sender = new EmailSender(smtpHost.trim(), smtpPort.trim(), fromAddress.trim(), userName.trim(), password.trim(), recepientEmails);
                    sender.sendMail(subject, bodyFiles, files, reportPaths);
                } catch (Exception e) {
                    actions.add(new MessageClientAction("Не удалось отправить почту","Отсылка писем"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
