package platform.server.mail;

import jasperapi.ReportGenerator;
import jasperapi.ReportHTMLExporter;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyMapImplement;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
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
    public static enum Format {PDF, DOCX, HTML, RTF}
    private static Map<Format, String> extensions = new HashMap<Format, String>();
    static {
        extensions.put(Format.PDF,  ".pdf");
        extensions.put(Format.DOCX, ".docx");
        extensions.put(Format.HTML, ".html");
        extensions.put(Format.RTF,  ".rtf");
    }

    public static enum FormStorageType {INLINE, ATTACH}

    private final List<FormEntity> forms = new ArrayList<FormEntity>();
    private final List<Format> formats = new ArrayList<Format>();
    private final List<FormStorageType> types = new ArrayList<FormStorageType>();
    private final List<Map<ObjectEntity, ClassPropertyInterface>> mapObjects = new ArrayList<Map<ObjectEntity, ClassPropertyInterface>>();
    private final List<LP> attachmentNames = new ArrayList<LP>();

    private final List<PropertyMapImplement<?, ClassPropertyInterface>> recepients = new ArrayList<PropertyMapImplement<?, ClassPropertyInterface>>();

    private final String subject;
    private final LP fromAddress;

    private final BusinessLogics<?> BL; // для возможности работы с формами в автоматическом режиме

    public EmailActionProperty(String sID, String caption, String mailSubject, LP fromAddress, BusinessLogics<?> BL, ValueClass[] classes) {
        super(sID, caption, getValueClassList(mailSubject, classes));

        this.subject = mailSubject;
        this.fromAddress = fromAddress;
        this.BL = BL;

        askConfirm = true;
        setImage("/images/email.png");
    }

    private static ValueClass[] getValueClassList(String mailSubject, ValueClass[] classes) {
        boolean subjectInterface = mailSubject == null;
        ValueClass[] result = new ValueClass[classes.length + (subjectInterface ? 1 : 0)];
        System.arraycopy(classes, 0, result, 0, classes.length);
        if (subjectInterface)
            result[classes.length] = StringClass.get(100);
        return result;
    }

    public <R extends PropertyInterface> void addRecepient(PropertyMapImplement<R, ClassPropertyInterface> recepient) {
        recepients.add(recepient);
    }

    public void addInlineForm(FormEntity form, Map<ObjectEntity, ClassPropertyInterface> objects) {
        forms.add(form);
        formats.add(Format.HTML);
        types.add(FormStorageType.INLINE);
        mapObjects.add(objects);
        attachmentNames.add(null);
    }

    public void addAttachmentForm(FormEntity form, Format format, Map<ObjectEntity, ClassPropertyInterface> objects, LP attachmentName) {
        forms.add(form);
        formats.add(format);
        types.add(FormStorageType.ATTACH);
        mapObjects.add(objects);
        attachmentNames.add(attachmentName);
    }

    private static JRAbstractExporter createExporter(Format format) {
        JRAbstractExporter exporter;
        switch (format) {
            case PDF:
                exporter = new JRPdfExporter();
                break;
            case DOCX:
                exporter = new JRDocxExporter();
                break;
            case RTF:
                exporter = new JRRtfExporter();
                break;
            default:
                exporter = new ReportHTMLExporter();
                exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, false);
                break;
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

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects, boolean groupLast) throws SQLException {

        try {
            if (BL.LM.disableEmail.read(session) != null) {
                EmailSender.logger.error("Отсылка почты отключена");
                return;
            }

            List<EmailSender.AttachmentProperties> attachmentForms = new ArrayList<EmailSender.AttachmentProperties>();
            List<String> inlineForms = new ArrayList<String>();
            Map<ByteArray, String> attachmentFiles = new HashMap<ByteArray, String>();

            for (int i = 0; i < forms.size(); i++) {
                Map<ObjectEntity, DataObject> formObjects = BaseUtils.join(mapObjects.get(i), keys);
                RemoteFormInterface remoteForm;
                if(executeForm!=null)
                    remoteForm = executeForm.createForm(forms.get(i), formObjects);
                else
                    remoteForm = BL.createForm(session, forms.get(i), formObjects);
                if(remoteForm!=null) { // если объекты подошли
                    ReportGenerator report = new ReportGenerator(remoteForm);
                    JasperPrint print = report.createReport(false, types.get(i) == FormStorageType.INLINE, attachmentFiles);
                    String filePath = createReportFile(print, formats.get(i));
                    if (types.get(i) == FormStorageType.INLINE) {
                        inlineForms.add(filePath);
                    } else {
                        String attachmentName = null;
                        LP attachmentProp = attachmentNames.get(i);
                        if (attachmentProp != null) {
                            // считаем, что для attachmentProp входы идут ровно также как и у ActionProp - в той же последовательности
                            int intSize = attachmentProp.listInterfaces.size();
                            DataObject[] input = new DataObject[intSize];
                            for (int j = 0; j < intSize; j++)
                                input[j] = keys.get(((List)interfaces).get(j));
                            attachmentName = (String)attachmentProp.read(session, modifier, input);
                        }
                        if (attachmentName == null)
                            attachmentName = forms.get(i).caption;
                        attachmentName = BaseUtils.rtrim(attachmentName.replace('"', '\''));
                        attachmentForms.add(new EmailSender.AttachmentProperties(filePath, attachmentName, formats.get(i)));
                    }
                }
            }

            Map<String, Message.RecipientType> recepientEmails = new HashMap<String, Message.RecipientType>();
            for(PropertyMapImplement<?, ClassPropertyInterface> recepient : recepients) {
                String recepientEmail = (String) recepient.read(session, keys, modifier);
                if(recepientEmail!=null)
                    recepientEmails.put(recepientEmail, MimeMessage.RecipientType.TO);
            }

            List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>)interfaces;

            String subject = (this.subject == null ? (String)keys.get(listInterfaces.get(listInterfaces.size()-1)).getValue() : this.subject);

            String smtpHost = (String) BL.LM.smtpHost.read(session, modifier);
            String smtpPort = (String) BL.LM.smtpPort.read(session, modifier);
            String fromAddress = (String) this.fromAddress.read(session, modifier);
            String userName = (String) BL.LM.emailAccount.read(session, modifier);
            String password = (String) BL.LM.emailPassword.read(session, modifier);
            String emailBlindCarbonCopy = (String) BL.LM.emailBlindCarbonCopy.read(session, modifier);
            if (emailBlindCarbonCopy != null && !emailBlindCarbonCopy.isEmpty() && !recepientEmails.containsKey(emailBlindCarbonCopy)) {
                recepientEmails.put(emailBlindCarbonCopy, MimeMessage.RecipientType.BCC);
            }

            if(smtpHost==null || fromAddress==null) {
                String errorMessage = "Не задан SMTP хост или адрес отправителя. Письма отосланы не будут.";
                EmailSender.logger.error(errorMessage);
                actions.add(new MessageClientAction(errorMessage, "Отсылка писем"));
            } else {
                EmailSender sender = new EmailSender(smtpHost.trim(), BaseUtils.nullTrim(smtpPort), fromAddress.trim(), BaseUtils.nullTrim(userName), BaseUtils.nullTrim(password), recepientEmails);
                sender.sendMail(subject, inlineForms, attachmentForms, attachmentFiles);
            }
        } catch (Exception e) {
            String errorMessage = "Не удалось отправить почту : " + e.toString();
            EmailSender.logger.error(errorMessage);
            actions.add(new MessageClientAction(errorMessage, "Отсылка писем"));
            e.printStackTrace();
        }
    }

}
