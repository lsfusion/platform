package lsfusion.server.logics.property.actions.form;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.awt.*;

import static lsfusion.server.logics.property.derived.DerivedProperty.createAnd;
import static lsfusion.server.logics.property.derived.DerivedProperty.createTrue;

public abstract class FormToolbarActionProperty extends ScriptingActionProperty {
    public final static Dimension BUTTON_SIZE = new Dimension(25, 20);

    private final boolean showCaption;

    @Override
    protected boolean isVolatile() { // проще чем разбираться, что используется
        return true;
    }

    public FormToolbarActionProperty(ScriptingLogicsModule lm) {
        this(lm, true);
    }

    public FormToolbarActionProperty(ScriptingLogicsModule lm, boolean showCaption) {
        super(lm);
        this.showCaption = showCaption;
    }

    protected CalcProperty getEnableIf() {
        return null;
    }

    protected LCP getShowIf() {
        return null;
    }

    @Override
    public CalcPropertyMapImplement<?, ClassPropertyInterface> getWhereProperty() {
        CalcProperty enableIf = getEnableIf();
        return enableIf == null ? super.getWhereProperty() : enableIf.getImplement();
    }

    private void setupToolbarButton(FormEntity form, PropertyDrawEntity propertyDraw) {
        LCP propertyCaption = getShowIf();
        if (propertyCaption != null) {
            propertyDraw.propertyShowIf = form.addPropertyObject(propertyCaption);
        }
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> propertyDraw, FormEntity<?> form, Version version) {
        super.proceedDefaultDraw(propertyDraw, form, version);

        setupToolbarButton(form, propertyDraw);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);

        if (!showCaption) {
            propertyView.caption = "";
        }
    }

    static LCP createShowIfProperty(final CalcProperty showIfs[], boolean showIfNots[]) {
        assert showIfs != null && showIfNots != null && showIfs.length == showIfNots.length;

        MList<CalcPropertyInterfaceImplement<PropertyInterface>> mAnds = ListFact.mList(showIfs.length);
        MList<Boolean> mNots = ListFact.mList(showIfs.length);

        for (int i = 0; i < showIfs.length; ++i) {
            mAnds.add(showIfs[i].getImplement());
            mNots.add(showIfNots[i]);
        }

        CalcPropertyMapImplement showIfImplement = createAnd(SetFact.<PropertyInterface>EMPTY(), createTrue(), mAnds.immutableList(), mNots.immutableList());
        return new LCP(showIfImplement.property);
    }
}
