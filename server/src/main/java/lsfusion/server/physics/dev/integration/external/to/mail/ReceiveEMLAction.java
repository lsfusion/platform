package lsfusion.server.physics.dev.integration.external.to.mail;

import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.util.FolderClosedIOException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;

import javax.mail.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.util.*;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ReceiveEMLAction extends EmailAction {
    private final ClassPropertyInterface accountInterface;

    public ReceiveEMLAction(EmailLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        accountInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        if(context.getDbManager().isServer()) {

            try {

                DataObject accountObject = context.getDataKeyValue(accountInterface);
                if (emailLM.disableAccount.read(context, accountObject) != null) {
                    logError(context, localize("{mail.disabled}"));
                    return;
                }

                String receiveHostAccount = (String) emailLM.receiveHostAccount.read(context, accountObject);
                Integer receivePortAccount = (Integer) emailLM.receivePortAccount.read(context, accountObject);
                String nameAccount = (String) emailLM.nameAccount.read(context, accountObject);
                String passwordAccount = (String) emailLM.passwordAccount.read(context, accountObject);
                AccountType accountType = AccountType.get((String) emailLM.nameReceiveAccountTypeAccount.read(context, accountObject));
                boolean startTLS = emailLM.startTLS.read(context, accountObject) != null;
                boolean deleteMessagesAccount = emailLM.deleteMessagesAccount.read(context, accountObject) != null;
                Integer lastDaysAccount = (Integer) emailLM.lastDaysAccount.read(context, accountObject);
                Integer maxMessagesAccount = (Integer) emailLM.maxMessagesAccount.read(context, accountObject);

                if (receiveHostAccount == null) {
                    logError(context, localize("{mail.pop3.host.not.specified.letters.will.not.be.received}"));
                    return;
                }

                boolean ignoreExceptions = LM.findProperty("ignoreExceptions[Account]").read(context, accountObject) != null;

                Set<Long> skipEmails = getSkipEmails(context, nameAccount);

                Map<Long, FileData> emlMap = receiveEML(context, skipEmails, ignoreExceptions, accountType, startTLS, receivePortAccount, nameAccount, passwordAccount, receiveHostAccount, lastDaysAccount, maxMessagesAccount, deleteMessagesAccount);
                for (Map.Entry<Long, FileData> entry : emlMap.entrySet()) {
                    DataObject entryObject = new DataObject(entry.getKey());
                    LM.findProperty("emlFile[LONG]").change(entry.getValue(), context, entryObject);
                }

            } catch (Exception e) {
                logger.error(localize("{mail.failed.to.receive.mail}"), e);
                context.delayUserInterfaction(new MessageClientAction(localize("{mail.failed.to.receive.mail}") + " : " + e, localize("{mail.receiving}")));
            }
        } else {
            logger.info("Email Server disabled, change serverComputer() to enable");
        }
    }

    private Set<Long> getSkipEmails(ExecutionContext context, String nameAccount) {
        Set<Long> skipEmails = new HashSet<>();
        try {
            KeyExpr emailExpr = new KeyExpr("email");
            ImRevMap<Object, KeyExpr> emailKeys = MapFact.singletonRev("email", emailExpr);

            QueryBuilder<Object, Object> emailQuery = new QueryBuilder<>(emailKeys);
            emailQuery.addProperty("uid", LM.findProperty("uid[Email]").getExpr(emailExpr));
            emailQuery.and(LM.findProperty("uid[Email]").getExpr(emailExpr).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> emailResult = emailQuery.execute(context);
            for (ImMap<Object, Object> entry : emailResult.values()) {
                skipEmails.add((Long) entry.get("uid"));
            }

        } catch (Exception e) {
            ServerLoggers.mailLogger.error(String.format("Account %s: read emails from base failed", nameAccount), e);
        }
        return skipEmails;
    }

    public Map<Long, FileData> receiveEML(ExecutionContext context, Set<Long> skipEmails, boolean ignoreExceptions, AccountType accountType, boolean startTLS, Integer receivePort, String user, String password, String receiveHost, Integer lastDays, Integer maxMessages, boolean deleteMessages) throws MessagingException, IOException, GeneralSecurityException {

        Map<Long, FileData> emlMap = new HashMap<>();

        Store emailStore = EmailReceiver.getEmailStore(receiveHost, accountType, startTLS);
        if (receivePort != null) emailStore.connect(receiveHost, receivePort, user, password);
        else emailStore.connect(receiveHost, user, password);

        List<Folder> folders = getSubFolders(emailStore.getFolder("INBOX"));

        for (Folder folder : folders) {

            folder.open(Folder.READ_WRITE);

            Timestamp minDateTime = null;
            if (lastDays != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -lastDays);
                minDateTime = new Timestamp(calendar.getTime().getTime());
            }

            int count = 0;
            int messageCount = folder.getMessageCount();
            while (count < messageCount && (maxMessages == null || count < maxMessages)) {
                try {
                    Message message = folder.getMessage(messageCount - count);
                    Timestamp dateTimeSentEmail = getSentDate(message);
                    if (minDateTime == null || dateTimeSentEmail == null || minDateTime.compareTo(dateTimeSentEmail) <= 0) {
                        Long uid = getUID(folder, message);
                        if (!skipEmails.contains(uid)) {
                            message.setFlag(deleteMessages ? Flags.Flag.DELETED : Flags.Flag.SEEN, true);
                            FileData emlFileEmail = new FileData(getEMLByteArray(message), "eml");
                            emlMap.put(uid, emlFileEmail);

                        }
                    }
                    count++;
                } catch (FolderClosedIOException e) {
                    ServerLoggers.mailLogger.error("Ignored exception :", e);
                    folder.open(Folder.READ_WRITE);
                } catch (Exception e) {
                    if (ignoreExceptions) {
                        ServerLoggers.mailLogger.error("Ignored exception :", e);
                        context.delayUserInterfaction(new MessageClientAction(e.toString(), localize("{mail.receiving}")));
                        count++;
                    } else throw e;
                }
            }

            folder.close(true);
        }
        emailStore.close();

        return emlMap;
    }

    private List<Folder> getSubFolders(Folder folder) throws MessagingException {
        List<Folder> folders = new ArrayList<>();
        folders.add(folder);
        //pop3 doesn't allow subfolders
        if (!(folder instanceof POP3Folder)) {
            for (Folder f : folder.list()) {
                folders.addAll(getSubFolders(f));
            }
        }
        return folders;
    }

    private Long getUID(Folder folder, Message message) throws MessagingException {
        UIDFolder uf = (UIDFolder) folder; // cast folder to UIDFolder interface
        return uf.getUID(message); // get message Id
    }

    private RawFileData getEMLByteArray(Message msg) throws IOException, MessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out); //вообще, out сначала необходимо MimeUtility.encode, а при открытии - decode, чтобы всё сохранялось корректно
        return new RawFileData(out);
    }
}