package platform.server.logics.property.actions.form;

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
import java.util.ArrayList;
import java.util.List;

import static platform.server.logics.property.derived.DerivedProperty.*;

public abstract class FormToolbarActionProperty extends SystemActionProperty {
    public final static Dimension BUTTON_SIZE = new Dimension(25, 20);

    private final CalcProperty enableIf;
    private final CalcProperty showIfs[];
    private final boolean showIfNots[];
    private final boolean showCaption;

    @Override
    protected boolean isVolatile() { // проще чем разбираться, что используется
        return true;
    }

    public FormToolbarActionProperty(String sid, String caption, boolean showCaption) {
        this(sid, caption, showCaption, null, null, null);
    }

    public FormToolbarActionProperty(String sid, String caption, CalcProperty enableIf, CalcProperty... showIfs) {
        this(sid, caption, true, enableIf, showIfs, showIfs == null ? null : new boolean[showIfs.length]);
    }

    public FormToolbarActionProperty(String sid, String caption, CalcProperty enableIf, CalcProperty[] showIfs, boolean[] showIfNots) {
        this(sid, caption, true, enableIf, showIfs, showIfNots);

    }
    public FormToolbarActionProperty(String sid, String caption, boolean showCaption, CalcProperty enableIf, CalcProperty[] showIfs, boolean[] showIfNots) {
        super(sid, caption, new ValueClass[0]);

        assert showIfs == null || showIfs.length == showIfNots.length;

        this.showCaption = showCaption;
        this.enableIf = enableIf;
        this.showIfs = showIfs;
        this.showIfNots = showIfNots;
    }

    @Override
    public CalcPropertyMapImplement<?, ClassPropertyInterface> getWhereProperty() {
        return enableIf == null ? super.getWhereProperty() : enableIf.getImplement();
    }

    private void setupToolbarButton(FormEntity form, PropertyDrawEntity propertyDraw) {
        if (showIfs == null) {
            return;
        }

        ArrayList<PropertyInterface> interfaces = new ArrayList<PropertyInterface>();
        List<CalcPropertyInterfaceImplement<PropertyInterface>> ands = new ArrayList<CalcPropertyInterfaceImplement<PropertyInterface>>();
        List<Boolean> nots = new ArrayList<Boolean>();

        for (int i = 0; i < showIfs.length; ++i) {
            ands.add(showIfs[i].getImplement());
            nots.add(showIfNots[i]);
        }

        CalcPropertyMapImplement showIfImplement = createAnd(interfaces, createTrue(), ands, nots);

        propertyDraw.propertyCaption = form.addPropertyObject(
                new LCP(createAnd(new ArrayList(), createStatic(GlobalConstants.CAPTION_ORIGINAL), showIfImplement).property)
        );
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
}
