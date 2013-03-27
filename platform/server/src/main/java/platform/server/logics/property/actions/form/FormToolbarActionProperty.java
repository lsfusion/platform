package platform.server.logics.property.actions.form;

import platform.base.col.ListFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.mutable.MList;
import platform.interop.form.GlobalConstants;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.SystemActionProperty;

import java.awt.*;

import static platform.server.logics.property.derived.DerivedProperty.*;

public abstract class FormToolbarActionProperty extends SystemActionProperty {
    public final static Dimension BUTTON_SIZE = new Dimension(25, 20);

    private final boolean showCaption;

    @Override
    protected boolean isVolatile() { // проще чем разбираться, что используется
        return true;
    }

    public FormToolbarActionProperty(String sid, String caption) {
        this(sid, caption, true);
    }

    public FormToolbarActionProperty(String sid, String caption, boolean showCaption) {
        super(sid, caption, new ValueClass[0]);
        this.showCaption = showCaption;
    }

    protected CalcProperty getEnableIf() {
        return null;
    }

    protected LCP getPropertyCaption() {
        return null;
    }

    @Override
    public CalcPropertyMapImplement<?, ClassPropertyInterface> getWhereProperty() {
        CalcProperty enableIf = getEnableIf();
        return enableIf == null ? super.getWhereProperty() : enableIf.getImplement();
    }

    private void setupToolbarButton(FormEntity form, PropertyDrawEntity propertyDraw) {
        LCP propertyCaption = getPropertyCaption();
        if (propertyCaption != null) {
            propertyDraw.propertyCaption = form.addPropertyObject(propertyCaption);
        }
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> propertyDraw, FormEntity<?> form) {
        super.proceedDefaultDraw(propertyDraw, form);

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

        return new LCP(createAnd(SetFact.<PropertyInterface>EMPTY(), createStatic(GlobalConstants.CAPTION_ORIGINAL), showIfImplement).property);
    }
}
