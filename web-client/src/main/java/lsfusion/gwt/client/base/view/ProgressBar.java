package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

import static lsfusion.gwt.client.view.MainFrame.v5;

/**
 * A widget that displays progress on an arbitrary scale.
 *
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.progress { primary style } </li>
 * <li>.progress .progress-bar { the actual progress bar }
 * </li>
 * <li>.progress .progress { text on the bar } </li>
 * <li>.progress .progress-text-firstHalf { applied to text
 * when progress is less than 50 percent } </li>
 * <li>.progress .progress-text-secondHalf { applied to
 * text when progress is greater than 50 percent } </li>
 * </ul>
 */
public class ProgressBar extends Widget implements ResizableWidget {
    /**
     * A formatter used to format the text displayed in the progress bar widget.
     */
    public abstract static class TextFormatter {
        /**
         * Generate the text to display in the ProgressBar based on the current
         * value.
         *
         * Override this method to change the text displayed within the ProgressBar.
         *
         * @param bar the progress bar
         * @param curProgress the current progress
         * @return the text to display in the progress bar
         */
        protected abstract String getText(ProgressBar bar, double curProgress);
    }

    /**
     * The bar element that displays the progress.
     */
    private Element barElement;

    /**
     * The current progress.
     */
    private double curProgress;

    /**
     * The maximum progress.
     */
    private double maxProgress;

    /**
     * The minimum progress.
     */
    private double minProgress;

    /**
     * A boolean that determines if the text is visible.
     */
    private boolean textVisible = true;

    /**
     * The element that displays text on the page.
     */
    private Element textElement;

    /**
     * The current text formatter.
     */
    private TextFormatter textFormatter;

    /**
     * Create a progress bar with default range of 0 to 100.
     */
    public ProgressBar() {
        this(0.0, 100.0, 0.0);
    }

    /**
     * Create a progress bar with an initial progress and a default range of 0 to
     * 100.
     *
     * @param curProgress the current progress
     */
    public ProgressBar(double curProgress) {
        this(0.0, 100.0, curProgress);
    }

    /**
     * Create a progress bar within the given range.
     *
     * @param minProgress the minimum progress
     * @param maxProgress the maximum progress
     */
    public ProgressBar(double minProgress, double maxProgress) {
        this(minProgress, maxProgress, 0.0);
    }

    /**
     * Create a progress bar within the given range starting at the specified
     * progress amount.
     *
     * @param minProgress the minimum progress
     * @param maxProgress the maximum progress
     * @param curProgress the current progress
     */
    public ProgressBar(double minProgress, double maxProgress, double curProgress) {
        this(minProgress, maxProgress, curProgress, null);
    }

    /**
     * Create a progress bar within the given range starting at the specified
     * progress amount.
     *
     * @param minProgress the minimum progress
     * @param maxProgress the maximum progress
     * @param curProgress the current progress
     * @param textFormatter the text formatter
     */
    public ProgressBar(double minProgress, double maxProgress,
                       double curProgress, TextFormatter textFormatter) {
        this.minProgress = minProgress;
        this.maxProgress = maxProgress;
        this.curProgress = curProgress;
        setTextFormatter(textFormatter);

        // Create the outer shell
        setElement(Document.get().createDivElement());
        getElement().getStyle().setPosition(Style.Position.RELATIVE);
       GwtClientUtils.addClassName(this, "progress");

        // Create the bar element
        barElement = Document.get().createDivElement();
        getElement().appendChild(barElement);
        barElement.getStyle().setHeight(100, Style.Unit.PCT);
        GwtClientUtils.addClassName(barElement, "progress-bar");

        // Create the text element
        textElement = Document.get().createDivElement();
        getElement().appendChild(textElement);
        Style textElementStyle = textElement.getStyle();
        textElementStyle.setPosition(Style.Position.ABSOLUTE);
        textElementStyle.setTop(0, Style.Unit.PX);
        GwtClientUtils.addClassNames(textElement, "progress-text", "progress-text-firstHalf");

        // Set the current progress
        setProgress(curProgress);
    }

    /**
     * Get the maximum progress.
     *
     * @return the maximum progress
     */
    public double getMaxProgress() {
        return maxProgress;
    }

    /**
     * Get the minimum progress.
     *
     * @return the minimum progress
     */
    public double getMinProgress() {
        return minProgress;
    }

    /**
     * Get the current percent complete, relative to the minimum and maximum
     * values. The percent will always be between 0.0 - 1.0.
     *
     * @return the current percent complete
     */
    public double getPercent() {
        // If we have no range
        if (maxProgress <= minProgress) {
            return 0.0;
        }

        // Calculate the relative progress
        double percent = (curProgress - minProgress) / (maxProgress - minProgress);
        return Math.max(0.0, Math.min(1.0, percent));
    }

    /**
     * Get the current progress.
     *
     * @return the current progress
     */
    public double getProgress() {
        return curProgress;
    }

    /**
     * Get the text formatter.
     *
     * @return the text formatter
     */
    public TextFormatter getTextFormatter() {
        return textFormatter;
    }

    /**
     * Check whether the text is visible or not.
     *
     * @return true if the text is visible
     */
    public boolean isTextVisible() {
        return textVisible;
    }

    /**
     * This method is called when the dimensions of the parent element change.
     * Subclasses should override this method as needed.
     *
     * Move the text to the center of the progress bar.
     *
     * @param width the new client width of the element
     * @param height the new client height of the element
     */
    public void onResize(int width, int height) {
        if (textVisible) {
            int textWidth = textElement.getOffsetWidth();
            int left = (width / 2) - (textWidth / 2);
            textElement.getStyle().setLeft(Math.max(left, 0), Style.Unit.PX);
        }
    }

    /**
     * Redraw the progress bar when something changes the layout.
     */
    public void redraw() {
        if (isAttached()) {
            int width = getElement().getClientWidth();
            int height = getElement().getClientHeight();
            onResize(width, height);
        }
    }

    /**
     * Set the maximum progress. If the minimum progress is more than the current
     * progress, the current progress is adjusted to be within the new range.
     *
     * @param maxProgress the maximum progress
     */
    public void setMaxProgress(double maxProgress) {
        this.maxProgress = maxProgress;
        curProgress = Math.min(curProgress, maxProgress);
        resetProgress();
    }

    /**
     * Set the minimum progress. If the minimum progress is more than the current
     * progress, the current progress is adjusted to be within the new range.
     *
     * @param minProgress the minimum progress
     */
    public void setMinProgress(double minProgress) {
        this.minProgress = minProgress;
        curProgress = Math.max(curProgress, minProgress);
        resetProgress();
    }

    /**
     * Set the current progress.
     *
     * @param curProgress the current progress
     */
    public void setProgress(double curProgress) {
        this.curProgress = Math.max(minProgress, Math.min(maxProgress, curProgress));

        // Calculate percent complete
        int percent = (int) (100 * getPercent());
        barElement.getStyle().setWidth(percent, Style.Unit.PCT);
        String text = generateText(curProgress);
        textElement.setInnerHTML(text);
        textElement.setTitle(text);

        // Set the style depending on the size of the bar
        if(percent < 50)
            GwtClientUtils.addClassName(textElement, "progress-text", "progress-text-firstHalf", v5);
        else
            GwtClientUtils.addClassName(textElement, "progress-text", "progress-text-secondHalf", v5);

        // Realign the text
        redraw();
    }

    /**
     * Set the text formatter.
     *
     * @param textFormatter the text formatter
     */
    public void setTextFormatter(TextFormatter textFormatter) {
        this.textFormatter = textFormatter;
    }

    /**
     * Sets whether the text is visible over the bar.
     *
     * @param isVisible True to show text, false to hide it
     */
    public void setTextVisible(boolean isVisible) {
        this.textVisible = isVisible;
        if (this.textVisible) {
            textElement.getStyle().clearDisplay();
            redraw();
        } else {
            textElement.getStyle().setDisplay(Style.Display.NONE);
        }
    }

    /**
     * Generate the text to display within the progress bar. Override this
     * function to change the default progress percent to a more informative
     * message, such as the number of kilobytes downloaded.
     *
     * @param curProgress the current progress
     * @return the text to display in the progress bar
     */
    protected String generateText(double curProgress) {
        if (textFormatter != null) {
            return textFormatter.getText(this, curProgress);
        } else {
            return (int) (100 * getPercent()) + "%";
        }
    }

    /**
     * Get the bar element.
     *
     * @return the bar element
     */
    protected Element getBarElement() {
        return barElement;
    }

    /**
     * Get the text element.
     *
     * @return the text element
     */
    protected Element getTextElement() {
        return textElement;
    }

    /**
     * This method is called immediately after a widget becomes attached to the
     * browser's document.
     */
    @Override
    protected void onLoad() {
        // Reset the position attribute of the parent element
        getElement().getStyle().setPosition(Style.Position.RELATIVE);
        ResizableWidgetCollection.get().add(this);
        redraw();
    }

    @Override
    protected void onUnload() {
        ResizableWidgetCollection.get().remove(this);
    }

    /**
     * Reset the progress text based on the current min and max progress range.
     */
    protected void resetProgress() {
        setProgress(getProgress());
    }
}