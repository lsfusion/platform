package lsfusion.server.data.sql.syntax;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

// the code that builds SQL puts no string between quotes by itself: a string goes into the SQL text through
// SQLSyntax.getStringLiteral, which escapes it, or into the query as a value, which is inlined only when it is safe
public class HandMadeStringLiteralTest {

    // a Java string that ends with a quote joined to what follows it, or one that starts with a quote joined to what
    // precedes it: "... '" + x, x + "' ..."
    private static final Pattern QUOTE_JOINED = Pattern.compile("'\"\\s*\\+|\\+\\s*\"'");

    // a statement written as a format whose argument goes between quotes: "... WHERE name='%s'"
    private static final Pattern FORMAT_QUOTED = Pattern.compile("'%(\\d+\\$)?s'");
    private static final Pattern SQL_WORD = Pattern.compile("\\b(SELECT|INSERT|UPDATE|DELETE|WHERE|SET|ALTER|CREATE|DROP|EXEC)\\b");

    // a source that builds SQL: one that names the syntax or a formula, runs a statement, is a part of the data layer,
    // or is a class whose values and casts are written into SQL
    private static final Pattern BUILDS_SQL = Pattern.compile("SQLSyntax|FormulaExpr|FormulaImpl|CustomFormulaSyntax|java\\.sql\\.Statement|executeSelect|executeDDL|^package lsfusion\\.server\\.data\\.|^package lsfusion\\.server\\.logics\\.classes\\.", Pattern.MULTILINE);

    // a line that makes a message: an exception, an error or a log record
    private static final Pattern MAKES_MESSAGE = Pattern.compile("(Exception|Error)\\(|startLog|[Ll]ogger\\.");

    // the script parser: a formula's text there is the code of a logic, the quotes it joins are messages
    private static final String PARSER = "lsfusion/server/language/";

    // the lines that join a quote on purpose
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            // the escaping itself
            "lsfusion/server/data/sql/syntax/DefaultSQLSyntax.java: return \"'\" + string.replace(\"'\", \"''\") + \"'\";",
            "lsfusion/server/data/sql/syntax/PostgreSQLSyntax.java: return \"E'\" + string.replace(\"\\\\\", \"\\\\\\\\\").replace(\"'\", \"''\") + \"'\";",
            "lsfusion/server/data/sql/syntax/MySQLSQLSyntax.java: return \"'\" + string.replace(\"\\\\\", \"\\\\\\\\\").replace(\"'\", \"''\") + \"'\";",
            // the subqueries of a recursion, escaped by escapeSql
            "lsfusion/server/data/sql/syntax/PostgreSQLSyntax.java: \",'(\" + escapeSql(initialSelect) + \")'\" + \",'(\" + escapeSql(stepSelect) + \")'\" +",
            "lsfusion/server/data/sql/syntax/PostgreSQLSyntax.java: \",'(\" + escapeSql(stepSmallSelect) + \")'\" + \",\" + smallLimit +",
            // an SQL expression between the quoted parts of a file value, not a string
            "lsfusion/server/logics/classes/data/file/FileClass.java: return \"'\" + ScriptedStringUtils.wrapFile((needImage ? \"1\" : \"0\") + serializeString(), \"'\" + stringConcatenate + getEncode(value) + stringConcatenate + \"'\") + \"'\";",
            // a class's caption in its description, not SQL
            "lsfusion/server/logics/classes/data/DataClass.java: return getCanonicalName() + \" '\" + ThreadLocalContext.localize(caption) + \"'\";",
            "lsfusion/server/logics/classes/user/CustomClass.java: result += \" '\" + ThreadLocalContext.localize(caption) + \"'\";"
    ));

    @Test
    public void noHandMadeStringLiterals() throws IOException {
        Path sources = Paths.get("src/main/java");
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList())) {
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                if (!BUILDS_SQL.matcher(text).find())
                    continue;
                String name = sources.relativize(file).toString().replace('\\', '/');
                if (name.startsWith(PARSER))
                    continue;
                String[] lines = text.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    boolean quoted = QUOTE_JOINED.matcher(line).find() || FORMAT_QUOTED.matcher(line).find() && SQL_WORD.matcher(line).find();
                    if (quoted && !MAKES_MESSAGE.matcher(line).find() && !ALLOWED.contains(name + ": " + line))
                        found.add(name + ":" + (i + 1) + ": " + line);
                }
            }
        }
        assertTrue("a string is put between quotes into SQL by hand - write it with SQLSyntax.getStringLiteral, or pass it as a value of the query:\n" +
                String.join("\n", found), found.isEmpty());
    }
}
