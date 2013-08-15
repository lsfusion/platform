package lsfusion.interop.form.layout;

import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ContextObject;

import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SimplexConstraints<T> extends ContextObject implements Serializable {

    public static final SimplexConstraints DEFAULT_CONSTRAINT = new SimplexConstraints();

    public DoNotIntersectSimplexConstraint childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
    public int maxVariables = 3;

//    public static int MAXIMUM = 1;
//    public static int PREFERRED = 0;

    // значение > 0 значит, что надо растягивать с определенным коэффициентом
    // значение == 0 значит, что нужно стремиться к preferredSize
    // значение < 0 значит, что нужно быть как можно меньше
    public double fillVertical = 0; //PREFERRED;
    public double fillHorizontal = 0; //PREFERRED;

    public static Insets DEFAULT_INSETS_SIBLING = new Insets(0, 0, 0, 0);
    public Insets insetsSibling = DEFAULT_INSETS_SIBLING;

    //приходится ставить хотя бы один вниз, иначе криво отрисовывает объекты снизу
    public static Insets DEFAULT_INSETS_INSIDE = new Insets(1, 0, 1, 1); //new Insets(1, 0, 1, 0);
    public Insets insetsInside = DEFAULT_INSETS_INSIDE;

    public static SimplexComponentDirections DEFAULT_DIRECTIONS = new SimplexComponentDirections(0.01, 0.01, 0, 0);
    public SimplexComponentDirections directions = DEFAULT_DIRECTIONS;

    // приходится делать сериализацию отдельно, посколько клиент будет работать не с исходным классом T, а с его ID
    transient public Map<T, DoNotIntersectSimplexConstraint> intersects = new HashMap<T, DoNotIntersectSimplexConstraint>();

    public SimplexConstraints() {
    }

    public SimplexConstraints(ApplicationContext context) {
        super(context);
    }

    public DoNotIntersectSimplexConstraint getChildConstraints() {
        return childConstraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimplexConstraints that = (SimplexConstraints) o;

        if (!childConstraints.equals(that.getChildConstraints()) ||
                maxVariables != that.maxVariables ||
                fillVertical != that.fillHorizontal ||
                fillHorizontal != that.fillHorizontal ||
                !insetsSibling.equals(that.insetsSibling) ||
                !insetsInside.equals(that.insetsInside) ||
                !directions.equals(that.directions) ||
                !intersects.equals(that.intersects))
            return false;

        return true;
    }

    public void setChildConstraints(DoNotIntersectSimplexConstraint childConstraints) {
        this.childConstraints = childConstraints;
        updateDependency(this, "childConstraints");
    }

    public String getFillVertical() {
        return String.valueOf(fillVertical);
    }

    public void setFillVertical(String fillVertical) {
        this.fillVertical = Double.parseDouble(fillVertical);
        updateDependency(this, "fillVertical");
    }

    public String getFillHorizontal() {
        return String.valueOf(fillHorizontal);
    }

    public void setFillHorizontal(String fillHorizontal) {
        this.fillHorizontal = Double.parseDouble(fillHorizontal);
        updateDependency(this, "fillHorizontal");
    }

    public String getMaxVariables() {
        return String.valueOf(maxVariables);
    }

    public void setMaxVariables(String maxVariables) {
        this.maxVariables = Integer.parseInt(maxVariables);
        updateDependency(this, "maxVariables");
    }

    public String getDirectionsTop() {
        return String.valueOf(directions.T);
    }

    public void setDirectionsTop(String directionTop) {
        this.directions.T = Double.parseDouble(directionTop);
        updateDependency(this, "directionsTop");
    }

    public String getDirectionsLeft() {
        return String.valueOf(directions.L);
    }

    public void setDirectionsLeft(String directionLeft) {
        this.directions.L = Double.parseDouble(directionLeft);
        updateDependency(this, "directionsLeft");
    }

    public String getDirectionsBottom() {
        return String.valueOf(directions.B);
    }

    public void setDirectionsBottom(String directionBottom) {
        this.directions.B = Double.parseDouble(directionBottom);
        updateDependency(this, "directionsBottom");
    }

    public String getDirectionsRight() {
        return String.valueOf(directions.R);
    }

    public void setDirectionsRight(String directionRight) {
        this.directions.R = Double.parseDouble(directionRight);
        updateDependency(this, "directionsRight");
    }

    //insetsSibling
    public String getInsetsSiblingTop() {
        return String.valueOf(insetsSibling.top);
    }

    public void setInsetsSiblingTop(String insetsSiblingTop) {
        this.insetsSibling.top = Integer.parseInt(insetsSiblingTop);
        updateDependency(this, "insetsSiblingTop");
    }

    public String getInsetsSiblingLeft() {
        return String.valueOf(insetsSibling.left);
    }

    public void setInsetsSiblingLeft(String insetsSiblingLeft) {
        this.insetsSibling.left = Integer.parseInt(insetsSiblingLeft);
        updateDependency(this, "insetsSiblingLeft");
    }

    public String getInsetsSiblingBottom() {
        return String.valueOf(insetsSibling.bottom);
    }

    public void setInsetsSiblingBottom(String insetsSiblingBottom) {
        this.insetsSibling.bottom = Integer.parseInt(insetsSiblingBottom);
        updateDependency(this, "insetsSiblingBottom");
    }

    public String getInsetsSiblingRight() {
        return String.valueOf(insetsSibling.right);
    }

    public void setInsetsSiblingRight(String insetsSiblingRight) {
        this.insetsSibling.right = Integer.parseInt(insetsSiblingRight);
        updateDependency(this, "insetsSiblingRight");
    }

    //insetsInside
    public String getInsetsInsideTop() {
        return String.valueOf(insetsInside.top);
    }

    public void setInsetsInsideTop(String insetsInsideTop) {
        this.insetsInside.top = Integer.parseInt(insetsInsideTop);
        updateDependency(this, "insetsInsideTop");
    }

    public String getInsetsInsideLeft() {
        return String.valueOf(insetsInside.left);
    }

    public void setInsetsInsideLeft(String insetsInsideLeft) {
        this.insetsInside.left = Integer.parseInt(insetsInsideLeft);
        updateDependency(this, "insetsInsideLeft");
    }

    public String getInsetsInsideBottom() {
        return String.valueOf(insetsInside.bottom);
    }

    public void setInsetsInsideBottom(String insetsInsideBottom) {
        this.insetsInside.bottom = Integer.parseInt(insetsInsideBottom);
        updateDependency(this, "insetsInsideBottom");
    }

    public String getInsetsInsideRight() {
        return String.valueOf(insetsInside.right);
    }

    public void setInsetsInsideRight(String insetsInsideRight) {
        this.insetsInside.right = Integer.parseInt(insetsInsideRight);
        updateDependency(this, "insetsInsideRight");
    }

    public Map<T, DoNotIntersectSimplexConstraint> getIntersects() {
        return this.intersects;
    }

    public void setIntersects(Map<T, DoNotIntersectSimplexConstraint> intersects) {
        this.intersects = intersects;
        updateDependency(this, "intersects");
    }

    public static <T> SimplexConstraints<T> getContainerDefaultConstraints(SimplexConstraints<T> constraints) {
        constraints.fillVertical = -1;
        constraints.fillHorizontal = -1;
        return constraints;
    }

    public static <T> SimplexConstraints<T> getGridDefaultConstraints(SimplexConstraints<T> constraints) {
        constraints.fillVertical = 1;
        constraints.fillHorizontal = 1;
        return constraints;
    }

    public static <T> SimplexConstraints<T> getTreeDefaultConstraints(SimplexConstraints<T> constraints) {
        constraints.fillVertical = 1;
        constraints.fillHorizontal = 1;
        return constraints;
    }

    public static <T> SimplexConstraints<T> getClassChooserDefaultConstraints(SimplexConstraints<T> constraints) {
        constraints.fillVertical = 1;
        constraints.fillHorizontal = 0.2;
        return constraints;
    }

    public static <T> SimplexConstraints<T> getPropertyDrawDefaultConstraints(SimplexConstraints<T> constraints) {
        constraints.insetsSibling = new Insets(0, 0, 2, 2);
        return constraints;
    }

    public static <T> SimplexConstraints<T> getRegularFilterGroupDefaultConstraints(SimplexConstraints<T> constraints) {
        constraints.insetsSibling = new Insets(0, 4, 2, 4);
        return constraints;
    }

    public static <T> SimplexConstraints<T> getShowTypeDefaultConstraints(SimplexConstraints<T> constraints) {
        constraints.directions =new SimplexComponentDirections(0.01, 0.0, 0.0, 0.01);
        return constraints;
    }
}
