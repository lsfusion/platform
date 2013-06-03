package fdk.utils.i18n;

import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.*;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface translationInterface;
    public final ClassPropertyInterface languageFromInterface;
    public final ClassPropertyInterface languageToInterface;

    public TranslateActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{StringClass.text, LM.getClassByName("Language"), LM.getClassByName("Language")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        translationInterface = i.next();
        languageFromInterface = i.next();
        languageToInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            DataSession session = context.getSession();

            DataObject translationEntry = context.getDataKeyValue(translationInterface);
            DataObject languageFromObject = context.getDataKeyValue(languageFromInterface);
            DataObject languageToObject = context.getDataKeyValue(languageToInterface);

            if (languageFromObject != null && languageToObject != null && translationEntry != null) {

                String languageFrom = (String) LM.findLCPByCompoundName("localeLanguage").read(session, languageFromObject);
                String languageTo = (String) LM.findLCPByCompoundName("localeLanguage").read(session, languageToObject);

                String url = "http://translate.google.com/translate_a/t?client=x&text=" + URLEncoder.encode(((String) translationEntry.object).trim(), "UTF-8") + "&sl=" + languageFrom.trim() + "&tl=" + languageTo.trim();
                URLConnection conn = new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");
                InputStreamReader r = new InputStreamReader(conn.getInputStream());
                StringBuilder s = new StringBuilder();
                char[] buf = new char[2048];
                while (true) {
                    int n = r.read(buf);
                    if (n < 0)
                        break;
                    s.append(buf, 0, n);
                }

                String result = "";
                String[] splitted = s.toString().split("\"trans\":\"");
                for (String line : splitted) {
                    Pattern pattern = Pattern.compile("(.*)\",\"orig\".*(\\}|\\{)");
                    Matcher m = pattern.matcher(line);
                    if (m.matches()) {
                        result += m.group(1).replace("\\n", "\n");
                    }
                }
                getLCP("translationResult").change(result, session);
            }

        } catch (ScriptingErrorLog.SemanticErrorException e) {
        } catch (MalformedURLException e) {
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

    }
}
