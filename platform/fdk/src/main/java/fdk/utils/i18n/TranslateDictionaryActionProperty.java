package fdk.utils.i18n;

import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateDictionaryActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface dictionaryInterface;
    public final ClassPropertyInterface termInterface;

    public TranslateDictionaryActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{LM.getClassByName("Dictionary"), StringClass.get(50)});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        dictionaryInterface = i.next();
        termInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            DataObject dictionaryObject = context.getKeyValue(dictionaryInterface);
            DataObject termObject = context.getKeyValue(termInterface);

            if (dictionaryObject != null && termObject != null) {

                Boolean insensitive = LM.findLCPByCompoundName("insensitiveDictionary").read(context.getSession(), dictionaryObject) != null;

                String source = (String) termObject.object;
                if (insensitive)
                    source = source.toUpperCase();
                if (source != null) {
                    String delim = ", .:;%#$@/\\|<>=+-_)(*&?^!~{}[]\"1234567890'";
                    StringTokenizer st = new StringTokenizer(source, delim, true);
                    String result = "";
                    String fullLineTranslation = (String) (LM.findLCPByCompoundName(insensitive ? "insensitiveTranslationDictionaryEntryDictionaryTerm" : "translationDictionaryEntryDictionaryTerm").read(context, dictionaryObject, new DataObject(source, StringClass.get(50))));
                    if (fullLineTranslation != null) {
                        result = fullLineTranslation.trim();
                    } else {
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            if (!delim.contains(token.subSequence(0, token.length()))) {
                                String translation = (String) (LM.findLCPByCompoundName(insensitive ? "insensitiveTranslationDictionaryEntryDictionaryTerm" : "translationDictionaryEntryDictionaryTerm").read(context, dictionaryObject, new DataObject(token, StringClass.get(50))));
                                if (translation != null) {
                                    token = translation.trim();
                                }
                            }
                            result += token;
                        }
                    }
                    LM.findLCPByCompoundName("translationResultString").change(result, context);
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
        }
    }
}
