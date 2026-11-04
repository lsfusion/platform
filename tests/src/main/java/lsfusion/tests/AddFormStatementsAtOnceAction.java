package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// two threads adding statements to one form at once, as two modules parsed at once that extend one form do. The form's
// list of statements is swapped for one that holds the first add inside it until the second thread either is inside too
// - two adds at once, which a plain list does not survive - or waits for the form the adds are made under. What the test
// saw goes into fsFailure, and so does a run that did not have the two at once
public class AddFormStatementsAtOnceAction extends AtOnceAction {

    public AddFormStatementsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormEntity form = context.getBL().findForm("FormStatements.fsForm");
        List<String> statements = form.formOrDesignStatementList;
        Hold hold = new Hold();
        List<String> holding = new ArrayList<String>() {
            @Override
            public boolean add(String statement) {
                return hold.call(() -> super.add(statement));
            }
        };
        form.formOrDesignStatementList = holding;
        String failure;
        try {
            failure = runAtOnce(context, hold, form, "two threads added statements to the form at once",
                    () -> form.addFormOrDesignStatementTokens(Arrays.asList("EXTEND FORM fsForm", ";")),
                    () -> form.addFormOrDesignStatementTokens(Arrays.asList("DESIGN fsForm", ";")));
        } finally {
            form.formOrDesignStatementList = statements;
        }
        List<String> added = new ArrayList<>(holding);
        Collections.sort(added);
        if (failure == null && !added.equals(Arrays.asList("DESIGN fsForm;", "EXTEND FORM fsForm;")))
            failure = "the form kept the statements " + added;

        try {
            findProperty("fsFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
