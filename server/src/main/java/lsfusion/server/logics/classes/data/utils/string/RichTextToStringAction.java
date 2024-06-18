package lsfusion.server.logics.classes.data.utils.string;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Iterator;

public class RichTextToStringAction extends InternalAction {
    private final ClassPropertyInterface richTextInterface;

    public RichTextToStringAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        richTextInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String value = (String) context.getDataKeyValue(richTextInterface).getValue();
        try {
            findProperty("resultString[]").change(cleanHTML(value), context);
        } catch (ScriptingErrorLog.SemanticErrorException | UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    private String cleanHTML(String value) throws UnsupportedEncodingException {
        if(value != null) {
            value = URLDecoder.decode(replaceSpecialCharacters(value), "UTF-8");
            //create Jsoup document from HTML
            Document jsoupDoc = Jsoup.parse(value);
            Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
            //set pretty print to false, so \n is not removed
            jsoupDoc.outputSettings(outputSettings);
            //select tags and append \n after that
            for(String tag : new String[] {"br", "ol", "ul"}) {
                jsoupDoc.select(tag).after("\\n");
            }
            //select tags and prepend \n before that
            for(String tag : new String[] {"p", "li", "blockquote", "div"}) {
                jsoupDoc.select(tag).before("\\n");
            }
            //get the HTML from the document, and retaining original new lines
            String str = jsoupDoc.html().replaceAll("\\\\n", "\n");
            value = Jsoup.clean(str, "", Whitelist.none(), outputSettings);
        }
        return value;
    }

    //https://stackoverflow.com/questions/6067673/urldecoder-illegal-hex-characters-in-escape-pattern-for-input-string
    private String replaceSpecialCharacters(String value) {
        value = value.replace((char) 160, ' '); //replace &nbsp;
        value = value.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        return value.replaceAll("\\+", "%2B");
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}