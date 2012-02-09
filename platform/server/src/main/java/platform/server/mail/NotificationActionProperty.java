package platform.server.mail;

import jasperapi.ReportGenerator;
import jasperapi.ReportHTMLExporter;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.Compare;
import platform.interop.action.MessageClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.classes.ActionClass;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.session.DataSession;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationActionProperty extends CustomActionProperty {

    private final LinkedHashMap<PropertyMapImplement<?, ClassPropertyInterface>, Message.RecipientType> recipients = new LinkedHashMap<PropertyMapImplement<?, ClassPropertyInterface>, Message.RecipientType>();

    private final String subjectNotification;
    private final String textNotification;
    private final String emailFromNotification;
    private final String emailToNotification;
    private final String emailToCCNotification;
    private final String emailToBCNotification;

    private final BusinessLogics<?> BL;

    public NotificationActionProperty(String sID, String caption, LP targetProperty, String subjectNotification, String textNotification, String emailFromNotification, String emailToNotification, String emailToCCNotification, String emailToBCNotification, BusinessLogics<?> BL) {
        super(sID, caption, getValueClasses(targetProperty));

        this.subjectNotification = subjectNotification;
        this.textNotification = textNotification;
        this.emailFromNotification = emailFromNotification;
        this.emailToNotification = emailToNotification;
        this.emailToCCNotification = emailToCCNotification;
        this.emailToBCNotification = emailToBCNotification;
        this.BL = BL;

        askConfirm = true;
        setImage("email.png");
    }

    private static ValueClass[] getValueClasses(LP sourceProperty) {
        return sourceProperty.getCommonClasses(new Result<ValueClass>() {
        });
    }

    public void execute(ExecutionContext context) throws SQLException {

        if (BL.LM.disableEmail.read(context) != null) {
            EmailSender.logger.error(ServerResourceBundle.getString("mail.sending.disabled"));
            return;
        }

        Map<String, Message.RecipientType> recipientEmails = new HashMap<String, Message.RecipientType>();

        if (emailToNotification != null)
            for (String email : emailToNotification.split(";")) {
                if (email != null && (!recipientEmails.containsKey(email.trim())))
                    recipientEmails.put(email.trim(), Message.RecipientType.TO);
            }
        if (emailToCCNotification != null)
            for (String email : emailToCCNotification.split(";")) {
                if (email != null && (!recipientEmails.containsKey(email.trim())))
                    recipientEmails.put(email.trim(), Message.RecipientType.CC);
            }
        if (emailToBCNotification != null)
            for (String email : emailToBCNotification.split(";")) {
                if (email != null && (!recipientEmails.containsKey(email.trim())))
                    recipientEmails.put(email.trim(), Message.RecipientType.BCC);
            }

        Pattern p = Pattern.compile("\\{(.*?)\\((.*?)\\)\\}");
        Matcher m = p.matcher(textNotification);
        String currentText = textNotification;
        while (m.find()) {
            String propertySID = m.group(1);
            int interfacesCount = m.group(2).split(",").length;
            LP replaceProperty = BL.LM.getLP(propertySID.trim());
            Object replacePropertyValue;
            if (!"".equals(m.group(2))) {
                DataObject[] objects = new DataObject[interfacesCount];
                for (int i = 0; i < interfacesCount; i++) {
                    for (int j = 0; j < context.getKeyCount(); j++) {
                        String sidFromReplaceProperty = getValueClasses(replaceProperty)[i].getSID();
                        String sidFromContext = ((ClassPropertyInterface) (context.getKeys().keySet().toArray()[j])).interfaceClass.getSID();
                        if (sidFromReplaceProperty.equals(sidFromContext))
                            objects[i] = (DataObject) context.getKeys().values().toArray()[j];
                    }
                }
                replacePropertyValue = replaceProperty.read(context, objects);
            } else
                replacePropertyValue = replaceProperty.read(context);
            if (replacePropertyValue != null)
                currentText = currentText.replace("$P{" + replaceProperty.property.getSID() + "(" + m.group(2) + ")}", replacePropertyValue.toString().trim());
        }

        List<EmailSender.AttachmentProperties> attachmentForms = new ArrayList<EmailSender.AttachmentProperties>();
        Map<ByteArray, String> attachmentFiles = new HashMap<ByteArray, String>();

        String encryptedConnectionType = (String) BL.LM.nameEncryptedConnectionType.read(context);
        String smtpHost = (String) BL.LM.smtpHost.read(context);
        String smtpPort = (String) BL.LM.smtpPort.read(context);
        String userName = (String) BL.LM.emailAccount.read(context);
        String password = (String) BL.LM.emailPassword.read(context);

        if (smtpHost == null || emailFromNotification == null) {
            String errorMessage = ServerResourceBundle.getString("mail.smtp.host.or.sender.not.specified.letters.will.not.be.sent");
            EmailSender.logger.error(errorMessage);
            context.addAction(new MessageClientAction(errorMessage, ServerResourceBundle.getString("mail.sending")));
        } else {
            EmailSender sender = new EmailSender(smtpHost.trim(),BaseUtils.nullTrim(smtpPort), encryptedConnectionType.trim(), emailFromNotification.trim(), BaseUtils.nullTrim(userName), BaseUtils.nullTrim(password), recipientEmails);
            try {
                sender.sendPlainMail(subjectNotification, currentText, attachmentForms, attachmentFiles);
            } catch (Exception e) {
                String errorMessage = ServerResourceBundle.getString("mail.failed.to.send.mail") + " : " + e.toString();
                EmailSender.logger.error(errorMessage);
                context.addAction(new MessageClientAction(errorMessage, ServerResourceBundle.getString("mail.sending")));
                e.printStackTrace();
            }
        }

    }
}
