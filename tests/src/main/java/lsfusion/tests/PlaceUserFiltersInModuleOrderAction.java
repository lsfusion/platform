package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// two modules that do not require each other giving one property of a form a user filter, the later one in the module
// order giving another property one before it. The filter's component is made by whichever of the two is parsed first,
// but where it stands in the FILTERS box has to follow the module order : after the other filter, where the later module
// puts it, in either order of the parse threads. And a module that requires the one giving the filter leaves it where
// that one's design put it, even giving it again from another group of the form. A form the server started with is
// finished and takes no more filters, so the test builds forms of its own. What the test saw goes into ufpFailure
public class PlaceUserFiltersInModuleOrderAction extends AtOnceAction {

    public PlaceUserFiltersInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    private interface Give {
        void give(AutoFinalFormEntity form, PropertyDrawEntity<?, ?> name, PropertyDrawEntity<?, ?> code);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);
            Version earlier = modules[0].getVersion();
            Version later = modules[1].getVersion();
            BaseLogicsModule baseLM = context.getBL().LM;

            List<String> earlierFirst = filtersBox(baseLM, (form, name, code) -> {
                form.addUserFilter(name, earlier);
                form.addUserFilter(code, later);
                form.addUserFilter(name, later);
            });
            List<String> laterFirst = filtersBox(baseLM, (form, name, code) -> {
                form.addUserFilter(code, later);
                form.addUserFilter(name, later);
                form.addUserFilter(name, earlier);
            });

            String failure = null;
            if (!earlierFirst.equals(laterFirst))
                failure = "the FILTERS box holds " + earlierFirst + " when " + modules[0].getName() + " is parsed first, but "
                        + laterFirst + " when " + modules[1].getName() + ", later in the module order, is";
            else if (!earlierFirst.equals(Arrays.asList("ufpCode", "ufpName")))
                failure = "the FILTERS box holds " + earlierFirst + ", not the filter of ufpName after the one of ufpCode, where "
                        + modules[1].getName() + ", later in the module order, puts it";
            else
                failure = movedByRequiring(baseLM);

            findProperty("ufpFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    // null when this module, which requires the base one, leaves where it is the filter the base module gave a property and
    // its design moved out of the FILTERS box - drawing the property in another group and giving it a filter again; else
    // where the filter went
    private String movedByRequiring(BaseLogicsModule baseLM) throws ScriptingErrorLog.SemanticErrorException {
        Version required = baseLM.getVersion();
        Version requiring = LM.getVersion();
        AutoFinalFormEntity form = new AutoFinalFormEntity(LocalizedString.create("Filter places"), baseLM);
        ObjectEntity object = form.addSingleGroupObject(findClass("UfpItem"));
        ObjectEntity other = form.addSingleGroupObject(findClass("UfpItem"));
        PropertyDrawEntity<?, ?> name = form.addPropertyDraw(findProperty("ufpName[UfpItem]"), SetFact.singletonOrder(object));
        form.addUserFilter(name, required);
        FilterView<?, ?> filter = form.view.get(name).filter;
        form.view.mainContainer.add(filter, required);

        name.setToDraw(other.groupTo, form, requiring);
        form.addUserFilter(name, requiring);
        ContainerView container = filter.getNFContainer(Version.current());
        return container == form.view.mainContainer ? null : "the filter the design of " + baseLM.getName() + " moved out of the FILTERS box went to "
                + (container != null ? container.getSID() : "no container") + ", given again by " + LM.getName() + ", which requires that module";
    }

    // the properties whose filters the FILTERS box of a new form holds, in their order, once `give` gave them filters
    private List<String> filtersBox(BaseLogicsModule baseLM, Give give) throws ScriptingErrorLog.SemanticErrorException {
        AutoFinalFormEntity form = new AutoFinalFormEntity(LocalizedString.create("Filter places"), baseLM);
        ObjectEntity object = form.addSingleGroupObject(findClass("UfpItem"));
        give.give(form, form.addPropertyDraw(findProperty("ufpName[UfpItem]"), SetFact.singletonOrder(object)),
                        form.addPropertyDraw(findProperty("ufpCode[UfpItem]"), SetFact.singletonOrder(object)));

        List<String> filters = new ArrayList<>();
        for (Object component : form.view.get(object.groupTo).grid.filtersContainer.children.getNFList(Version.current()))
            filters.add(((FilterView<?, ?>) component).property.entity.getInheritedProperty().getName());
        return filters;
    }
}
