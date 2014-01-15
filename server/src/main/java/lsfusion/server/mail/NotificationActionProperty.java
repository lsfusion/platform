package lsfusion.server.mail;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.NullValue;
import org.apache.log4j.Logger;
import lsfusion.base.BaseUtils;
import lsfusion.base.ByteArray;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import javax.mail.Message;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationActionProperty extends SystemExplicitActionProperty {
    private final static Logger logger = ServerLoggers.mailLogger;

    private final LinkedHashMap<CalcPropertyMapImplement<?, ClassPropertyInterface>, Message.RecipientType> recipients = new LinkedHashMap<CalcPropertyMapImplement<?, ClassPropertyInterface>, Message.RecipientType>();

    private final String subjectNotification;
    private final String textNotification;
    private final String emailFromNotification;
    private final String emailToNotification;
    private final String emailToCCNotification;
    private final String emailToBCNotification;

    private final EmailLogicsModule emailLM;

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return getUsedProps(ListFact.fromJavaCol(recipients.keySet()));
    }

    public NotificationActionProperty(String sID, String caption, LCP targetProperty, String subjectNotification, String textNotification, String emailFromNotification, String emailToNotification, String emailToCCNotification, String emailToBCNotification, EmailLogicsModule emailLM) {
        super(sID, caption, getValueClasses(targetProperty));

        this.subjectNotification = subjectNotification;
        this.textNotification = textNotification;
        this.emailFromNotification = emailFromNotification;
        this.emailToNotification = emailToNotification;
        this.emailToCCNotification = emailToCCNotification;
        this.emailToBCNotification = emailToBCNotification;

        this.emailLM = emailLM;

        askConfirm = true;
        setImage("email.png");
    }

    private static ValueClass[] getValueClasses(LCP sourceProperty) {
        return sourceProperty.getInterfaceClasses();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        if (emailLM.disableAccount.read(context) != null) {
            logger.error(ServerResourceBundle.getString("mail.disabled"));
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
            LCP replaceProperty = context.getBL().getLCP(propertySID.trim());
            Object replacePropertyValue;
            if (!"".equals(m.group(2))) {
                ObjectValue[] objects = new ObjectValue[interfacesCount];
                for (int i = 0; i < interfacesCount; i++) {
                    for (int j = 0; j < context.getKeyCount(); j++) {
                        String sidFromReplaceProperty = getValueClasses(replaceProperty)[i].getSID();
                        String sidFromContext = ((ClassPropertyInterface) (context.getKeys().getKey(j))).interfaceClass.getSID();
                        if (sidFromReplaceProperty.equals(sidFromContext))
                            objects[i] = context.getKeys().getValue(j);
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

        ObjectValue defaultAccount = emailLM.defaultInboxAccount.readClasses(context.getSession());

        if (!(defaultAccount instanceof NullValue)) {
            String encryptedConnectionType = (String) emailLM.nameEncryptedConnectionTypeAccount.read(context);
            String smtpHostAccount = (String) emailLM.smtpHostAccount.read(context);
            String smtpPortAccount = (String) emailLM.smtpPortAccount.read(context);
            String nameAccount = (String) emailLM.nameAccount.read(context);
            String password = (String) emailLM.passwordAccount.read(context);

            if (smtpHostAccount == null || emailFromNotification == null) {
                String errorMessage = ServerResourceBundle.getString("mail.smtp.host.or.sender.not.specified.letters.will.not.be.sent");
                logger.error(errorMessage);
                context.delayUserInterfaction(new MessageClientAction(errorMessage, ServerResourceBundle.getString("mail.sending")));
            } else {
                EmailSender sender = new EmailSender(smtpHostAccount.trim(), BaseUtils.nullTrim(smtpPortAccount), encryptedConnectionType.trim(), emailFromNotification.trim(), BaseUtils.nullTrim(nameAccount), BaseUtils.nullTrim(password), recipientEmails);
                try {
                    sender.sendPlainMail(subjectNotification, currentText, attachmentForms, attachmentFiles);
                } catch (Exception e) {
                    String errorMessage = ServerResourceBundle.getString("mail.failed.to.send.mail") + " : " + e.toString();
                    logger.error(errorMessage);
                    context.delayUserInterfaction(new MessageClientAction(errorMessage, ServerResourceBundle.getString("mail.sending")));
                    e.printStackTrace();
                }
            }
        }
    }
}
