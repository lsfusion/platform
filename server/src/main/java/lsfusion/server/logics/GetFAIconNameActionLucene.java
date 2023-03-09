package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetFAIconNameActionLucene extends InternalAction {

    private final ClassPropertyInterface searchPhraseInterface;

    private IndexWriter indexWriter = null;
    private final Directory ramDirectory = new ByteBuffersDirectory();


    public GetFAIconNameActionLucene(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        searchPhraseInterface = i.next();

        try {
            indexWriter = new IndexWriter(ramDirectory, new IndexWriterConfig(new EnglishAnalyzer()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String searchPhrase = (String) context.getDataKeyValue(searchPhraseInterface).getValue();

        try {
            ImMap<ImList<Object>, Object> terms = findProperty("terms[Icon]").readAll(context);
            ImMap<ImList<Object>, Object> iconNames = findProperty("iconName[Icon]").readAll(context);
            ImMap<ImList<Object>, Object> labels = findProperty("label[Icon]").readAll(context);

            for (ImList<Object> key : terms.keys()) {
                fillIndex((String) iconNames.get(key), terms.get(key) + " " + labels.get(key));
            }

            indexWriter.commit();

            IndexReader open = DirectoryReader.open(ramDirectory);
            IndexSearcher searcher = new IndexSearcher(open);
            TopDocs topDocs = searcher.search(new QueryBuilder(new EnglishAnalyzer()).createBooleanQuery("terms", String.join(" ", splitCamelCase(searchPhrase))), 1);

            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                StoredFields storedFields = searcher.storedFields();
                Document document = storedFields.document(scoreDoc.doc);

                findProperty("IconSearchLucene.bestIconName[]").change(document.get("iconName"), context);
                System.out.println(document.get("iconName"));
            }


        } catch (ScriptingErrorLog.SemanticErrorException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void fillIndex(String iconName, String term) {
        try {
            Document doc = new Document();

            Field iconNameField = new StoredField("iconName", iconName);
            doc.add(iconNameField);

            Field termsField = new TextField("terms", term, Field.Store.YES);
            doc.add(termsField);

            indexWriter.addDocument(doc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Pattern camelCasePattern = Pattern.compile("(([A-Z]?[a-z]+)|([A-Z]))");
    public List<String> splitCamelCase(String text) {
        Matcher matcher = camelCasePattern.matcher(text);
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group(0));
        }
        return words;
    }
}
