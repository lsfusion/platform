/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.impl.PopupImpl;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class PopupPanel extends SimplePanel implements HasAnimation, HasCloseHandlers<PopupPanel> {

  /**
   * A callback that is used to set the position of a {@link PopupPanel} right
   * before it is shown.
   */
  public interface PositionCallback {

    /**
     * Provides the opportunity to set the position of the PopupPanel right
     * before the PopupPanel is shown. The offsetWidth and offsetHeight values
     * of the PopupPanel are made available to allow for positioning based on
     * its size.
     *
     * @param offsetWidth the offsetWidth of the PopupPanel
     * @param offsetHeight the offsetHeight of the PopupPanel
     * @see PopupPanel#setPopupPositionAndShow(PositionCallback)
     */
    void setPosition(int offsetWidth, int offsetHeight);
  }

  /**
   * The type of animation to use when opening the popup.
   *
   * <ul>
   * <li>CENTER - Expand from the center of the popup</li>
   * <li>ONE_WAY_CORNER - Expand from the top left corner, do not animate hiding</li>
   * <li>ROLL_DOWN - Expand from the top to the bottom, do not animate hiding</li>
   * </ul>
   */
  public enum AnimationType {
    CENTER, ONE_WAY_CORNER, ROLL_DOWN
  }

  /**
   * An {@link Animation} used to enlarge the popup into view.
   */
  static class ResizeAnimation extends Animation {
    /**
     * The {@link PopupPanel} being affected.
     */
    private PopupPanel curPanel;

    /**
     * Indicates whether or not the {@link PopupPanel} is in the process of
     * unloading. If the popup is unloading, then the animation just does
     * cleanup.
     */
    private boolean isUnloading;

    /**
     * The offset height and width of the current {@link PopupPanel}.
     */
    private int offsetHeight, offsetWidth = -1;

    /**
     * A boolean indicating whether we are showing or hiding the popup.
     */
    private boolean showing;

    /**
     * The timer used to delay the show animation.
     */
    private Timer showTimer;

    /**
     * Create a new {@link ResizeAnimation}.
     *
     * @param panel the panel to affect
     */
    public ResizeAnimation(PopupPanel panel) {
      this.curPanel = panel;
    }

    /**
     * Open or close the content. This method always called immediately after
     * the PopupPanel showing state has changed, so we base the animation on the
     * current state.
     *
     * @param showing true if the popup is showing, false if not
     */
    public void setState(boolean showing, boolean isUnloading) {
      // Immediately complete previous open/close animation
      this.isUnloading = isUnloading;
      cancel();

      // If there is a pending timer to start a show animation, then just cancel
      // the timer and complete the show operation.
      if (showTimer != null) {
        showTimer.cancel();
        showTimer = null;
        onComplete();
      }

      // Update the logical state.
      curPanel.showing = showing;
      curPanel.updateHandlers();

      // Determine if we need to animate
      boolean animate = !isUnloading && curPanel.isAnimationEnabled;
      if (curPanel.animType != AnimationType.CENTER && !showing) {
        animate = false;
      }

      // Open the new item
      this.showing = showing;
      if (animate) {
        // impl.onShow takes some time to complete, so we do it before starting
        // the animation. If we move this to onStart, the animation will look
        // choppy or not run at all.
        if (showing) {

          // Set the position attribute, and then attach to the DOM. Otherwise,
          // the PopupPanel will appear to 'jump' from its static/relative
          // position to its absolute position (issue #1231).
          curPanel.getElement().getStyle().setProperty("position", "absolute");
          if (curPanel.topPosition != -1) {
            curPanel.setPopupPosition(curPanel.leftPosition,
                curPanel.topPosition);
          }
          impl.setClip(curPanel.getElement(), getRectString(0, 0, 0, 0));
          RootPanel.get().add(curPanel);

          // Wait for the popup panel and iframe to be attached before running
          // the animation. We use a Timer instead of a DeferredCommand so we
          // can cancel it if the popup is hidden synchronously.
          showTimer = new Timer() {
            @Override
            public void run() {
              showTimer = null;
              ResizeAnimation.this.run(ANIMATION_DURATION);
            }
          };
          showTimer.schedule(1);
        } else {
          run(ANIMATION_DURATION);
        }
      } else {
        onInstantaneousRun();
      }
    }

    @Override
    protected void onComplete() {
      if (!showing) {
        if (!isUnloading) {
          RootPanel.get().remove(curPanel);
        }
      }
      impl.setClip(curPanel.getElement(), "rect(auto, auto, auto, auto)");
      curPanel.getElement().getStyle().setProperty("overflow", "visible");
    }

    @Override
    protected void onStart() {
      offsetHeight = curPanel.getOffsetHeight();
      offsetWidth = curPanel.getOffsetWidth();
      curPanel.getElement().getStyle().setProperty("overflow", "hidden");
      super.onStart();
    }

    @Override
    protected void onUpdate(double progress) {
      if (!showing) {
        progress = 1.0 - progress;
      }

      // Determine the clipping size
      int top = 0;
      int left = 0;
      int right = 0;
      int bottom = 0;
      int height = (int) (progress * offsetHeight);
      int width = (int) (progress * offsetWidth);
      switch (curPanel.animType) {
        case ROLL_DOWN:
          right = offsetWidth;
          bottom = height;
          break;
        case CENTER:
          top = (offsetHeight - height) >> 1;
          left = (offsetWidth - width) >> 1;
          right = left + width;
          bottom = top + height;
          break;
        case ONE_WAY_CORNER:
          if (LocaleInfo.getCurrentLocale().isRTL()) {
            left = offsetWidth - width;
          }
          right = left + width;
          bottom = top + height;
          break;
      }
      // Set the rect clipping
      impl.setClip(curPanel.getElement(), getRectString(top, right, bottom, left));
    }

    /**
     * Returns a rect string.
     */
    private String getRectString(int top, int right, int bottom, int left) {
      return "rect(" + top + "px, " + right + "px, " + bottom + "px, " + left + "px)";
    }

    private void onInstantaneousRun() {
      if (showing) {
        // Set the position attribute, and then attach to the DOM. Otherwise,
        // the PopupPanel will appear to 'jump' from its static/relative
        // position to its absolute position (issue #1231).
        curPanel.getElement().getStyle().setProperty("position", "absolute");
        if (curPanel.topPosition != -1) {
          curPanel.setPopupPosition(curPanel.leftPosition, curPanel.topPosition);
        }
        RootPanel.get().add(curPanel);
      } else {
        if (!isUnloading) {
          RootPanel.get().remove(curPanel);
        }
      }
      curPanel.getElement().getStyle().setProperty("overflow", "visible");
    }
  }

  /**
   * The duration of the animation.
   */
  private static final int ANIMATION_DURATION = 200;

  private static final PopupImpl impl = GWT.create(PopupImpl.class);

  private AnimationType animType = AnimationType.CENTER;

  private boolean showing;

  private List<Element> autoHidePartners;

  // Used to track requested size across changing child widgets
  private String desiredHeight;

  private String desiredWidth;

  private boolean isAnimationEnabled = false;

  // the left style attribute in pixels
  private int leftPosition = -1;

  private HandlerRegistration nativePreviewHandlerRegistration;
  private HandlerRegistration historyHandlerRegistration;

  /**
   * The {@link ResizeAnimation} used to open and close the {@link PopupPanel}s.
   */
  private ResizeAnimation resizeAnimation = new ResizeAnimation(this);

  // The top style attribute in pixels
  private int topPosition = -1;

  private DecoratorPanel decPanel;

  @Override
  public Widget getWidget() {
    return decPanel.getWidget();
  }

  @Override
  public void setWidget(Widget w) {
    decPanel.setWidget(w);
    maybeUpdateSize();
  }

  public PopupPanel() {
    super();
    super.getContainerElement().appendChild(impl.createElement());

    // Default position of popup should be in the upper-left corner of the
    // window. By setting a default position, the popup will not appear in
    // an undefined location if it is shown before its position is set.
    setPopupPosition(0, 0);

    decPanel = new DecoratorPanel(new String[] {"suggestPopupTop", "suggestPopupMiddle", "suggestPopupBottom"}, 1);
    super.setWidget(decPanel);
    maybeUpdateSize();
  }

  public void addAutoHidePartner(Element partner) {
    assert partner != null : "partner cannot be null";
    if (autoHidePartners == null) {
      autoHidePartners = new ArrayList<>();
    }
    autoHidePartners.add(partner);
  }

  public HandlerRegistration addCloseHandler(CloseHandler<PopupPanel> handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  /**
   * Centers the popup in the browser window and shows it. If the popup was
   * already showing, then the popup is centered.
   */
  public void center() {
    boolean initiallyShowing = showing;
    boolean initiallyAnimated = isAnimationEnabled;

    if (!initiallyShowing) {
      setVisible(false);
      setAnimationEnabled(false);
      show();
    }

    // If left/top are set from a previous center() call, and our content
    // has changed, we may get a bogus getOffsetWidth because our new content
    // is wrapping (giving a lower offset width) then it would without the
    // previous left. Setting left/top back to 0 avoids this.
    Element elem = getElement();
    elem.getStyle().setPropertyPx("left", 0);
    elem.getStyle().setPropertyPx("top", 0);

    int left = (Window.getClientWidth() - getOffsetWidth()) >> 1;
    int top = (Window.getClientHeight() - getOffsetHeight()) >> 1;
    setPopupPosition(Math.max(Window.getScrollLeft() + left, 0), Math.max(
        Window.getScrollTop() + top, 0));

    if (!initiallyShowing) {
      setAnimationEnabled(initiallyAnimated);
      // Run the animation. The popup is already visible, so we can skip the
      // call to setState.
      if (initiallyAnimated) {
        impl.setClip(getElement(), "rect(0px, 0px, 0px, 0px)");
        setVisible(true);
        resizeAnimation.run(ANIMATION_DURATION);
      } else {
        setVisible(true);
      }
    }
  }

  /**
   * Gets the panel's offset height in pixels. Calls to
   * {@link #setHeight(String)} before the panel's child widget is set will not
   * influence the offset height.
   *
   * @return the object's offset height
   */
  @Override
  public int getOffsetHeight() {
    return super.getOffsetHeight();
  }

  /**
   * Gets the panel's offset width in pixels. Calls to {@link #setWidth(String)}
   * before the panel's child widget is set will not influence the offset width.
   *
   * @return the object's offset width
   */
  @Override
  public int getOffsetWidth() {
    return super.getOffsetWidth();
  }

  @Override
  public String getTitle() {
    return getContainerElement().getPropertyString("title");
  }

  /**
   * Hides the popup and detaches it from the page. This has no effect if it is
   * not currently showing.
   */
  public void hide() {
    hide(false);
  }

  /**
   * Hides the popup and detaches it from the page. This has no effect if it is
   * not currently showing.
   *
   * @param autoClosed the value that will be passed to
   *          {@link CloseHandler#onClose(CloseEvent)} when the popup is closed
   */
  public void hide(boolean autoClosed) {
    if (!isShowing()) {
      return;
    }
    resizeAnimation.setState(false, false);
    CloseEvent.fire(this, this, autoClosed);
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  /**
   * Determines whether or not this popup is showing.
   *
   * @return <code>true</code> if the popup is showing
   * @see #show()
   * @see #hide()
   */
  public boolean isShowing() {
    return showing;
  }

  /**
   * Determines whether or not this popup is visible. Note that this just checks
   * the <code>visibility</code> style attribute, which is set in the
   * {@link #setVisible(boolean)} method. If you want to know if the popup is
   * attached to the page, use {@link #isShowing()} instead.
   *
   * @return <code>true</code> if the object is visible
   * @see #setVisible(boolean)
   */
  @Override
  public boolean isVisible() {
    return !"hidden".equals(getElement().getStyle().getProperty("visibility"));
  }

  /**
   * Remove an autoHide partner.
   *
   * @param partner the auto hide partner to remove
   */
  public void removeAutoHidePartner(Element partner) {
    assert partner != null : "partner cannot be null";
    if (autoHidePartners != null) {
      autoHidePartners.remove(partner);
    }
  }

  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  /**
   * Sets the height of the panel's child widget. If the panel's child widget
   * has not been set, the height passed in will be cached and used to set the
   * height immediately after the child widget is set.
   *
   * <p>
   * Note that subclasses may have a different behavior. A subclass may decide
   * not to change the height of the child widget. It may instead decide to
   * change the height of an internal panel widget, which contains the child
   * widget.
   * </p>
   *
   * @param height the object's new height, in CSS units (e.g. "10px", "1em")
   */
  @Override
  public void setHeight(String height) {
    desiredHeight = height;
    maybeUpdateSize();
    // If the user cleared the size, revert to not trying to control children.
    if (height.length() == 0) {
      desiredHeight = null;
    }
  }

  /**
   * Sets the popup's position relative to the browser's client area. The
   * popup's position may be set before calling {@link #show()}.
   *
   * @param left the left position, in pixels
   * @param top the top position, in pixels
   */
  public void setPopupPosition(int left, int top) {
    // Save the position of the popup
    leftPosition = left;
    topPosition = top;

    // Account for the difference between absolute position and the
    // body's positioning context.
    left -= Document.get().getBodyOffsetLeft();
    top -= Document.get().getBodyOffsetTop();

    // Set the popup's position manually, allowing setPopupPosition() to be
    // called before show() is called (so a popup can be positioned without it
    // 'jumping' on the screen).
    Element elem = getElement();
    elem.getStyle().setPropertyPx("left", left);
    elem.getStyle().setPropertyPx("top", top);
  }

  /**
   * Sets the popup's position using a {@link PositionCallback}, and shows the
   * popup. The callback allows positioning to be performed based on the
   * offsetWidth and offsetHeight of the popup, which are normally not available
   * until the popup is showing. By positioning the popup before it is shown,
   * the popup will not jump from its original position to the new position.
   *
   * @param callback the callback to set the position of the popup
   * @see PositionCallback#setPosition(int offsetWidth, int offsetHeight)
   */
  public void setPopupPositionAndShow(PositionCallback callback) {
    setVisible(false);
    show();
    callback.setPosition(getOffsetWidth(), getOffsetHeight());
    setVisible(true);
  }

  @Override
  public void setTitle(String title) {
    Element containerElement = getContainerElement();
    if (title == null || title.length() == 0) {
      containerElement.removeAttribute("title");
    } else {
      containerElement.setAttribute("title", title);
    }
  }

  /**
   * Sets whether this object is visible. This method just sets the
   * <code>visibility</code> style attribute. You need to call {@link #show()}
   * to actually attached/detach the {@link PopupPanel} to the page.
   *
   * @param visible <code>true</code> to show the object, <code>false</code> to
   *          hide it
   * @see #show()
   * @see #hide()
   */
  @Override
  public void setVisible(boolean visible) {
    // We use visibility here instead of UIObject's default of display
    // Because the panel is absolutely positioned, this will not create
    // "holes" in displayed contents and it allows normal layout passes
    // to occur so the size of the PopupPanel can be reliably determined.
    getElement().getStyle().setProperty("visibility", visible ? "visible" : "hidden");
  }

/*  @Override
  public void setWidget(Widget w) {
    super.setWidget(w);
    maybeUpdateSize();
  }*/

  /**
   * Sets the width of the panel's child widget. If the panel's child widget has
   * not been set, the width passed in will be cached and used to set the width
   * immediately after the child widget is set.
   *
   * <p>
   * Note that subclasses may have a different behavior. A subclass may decide
   * not to change the width of the child widget. It may instead decide to
   * change the width of an internal panel widget, which contains the child
   * widget.
   * </p>
   *
   * @param width the object's new width, in CSS units (e.g. "10px", "1em")
   */
  @Override
  public void setWidth(String width) {
    desiredWidth = width;
    maybeUpdateSize();
    // If the user cleared the size, revert to not trying to control children.
    if (width.length() == 0) {
      desiredWidth = null;
    }
  }

  /**
   * Shows the popup and attach it to the page. It must have a child widget
   * before this method is called.
   */
  public void show() {
    if (showing) {
      return;
    } else if (isAttached()) {
      // The popup is attached directly to another panel, so we need to remove
      // it from its parent before showing it. This is a weird use case, but
      // since PopupPanel is a Widget, its legal.
      this.removeFromParent();
    }
    resizeAnimation.setState(true, false);
  }

  /**
   * Normally, the popup is positioned directly below the relative target, with
   * its left edge aligned with the left edge of the target. Depending on the
   * width and height of the popup and the distance from the target to the
   * bottom and right edges of the window, the popup may be displayed directly
   * above the target, and/or its right edge may be aligned with the right edge
   * of the target.
   *
   * @param target the target to show the popup below
   */
  public final void showRelativeTo(final Element target) {
    // Set the position of the popup right before it is shown.
    setPopupPositionAndShow((offsetWidth, offsetHeight) -> position(target, offsetWidth, offsetHeight));
  }

  @Override
  protected com.google.gwt.user.client.Element getContainerElement() {
    return impl.getContainerElement(getPopupImplElement()).cast();
  }

  @Override
  protected com.google.gwt.user.client.Element getStyleElement() {
    return impl.getStyleElement(getPopupImplElement()).cast();
  }

  @Override
  protected void onUnload() {
    super.onUnload();

    // Just to be sure, we perform cleanup when the popup is unloaded (i.e.
    // removed from the DOM). This is normally taken care of in hide(), but it
    // can be missed if someone removes the popup directly from the RootPanel.
    if (isShowing()) {
      resizeAnimation.setState(false, true);
    }
  }

  /**
   * We control size by setting our child widget's size. However, if we don't
   * currently have a child, we record the size the user wanted so that when we
   * do get a child, we can set it correctly. Until size is explicitly cleared,
   * any child put into the popup will be given that size.
   */
  void maybeUpdateSize() {
    // For subclasses of PopupPanel, we want the default behavior of setWidth
    // and setHeight to change the dimensions of PopupPanel's child widget.
    // We do this because PopupPanel's child widget is the first widget in
    // the hierarchy which provides structure to the panel. DialogBox is
    // an example of this. We want to set the dimensions on DialogBox's
    // FlexTable, which is PopupPanel's child widget. However, it is not
    // DialogBox's child widget. To make sure that we are actually getting
    // PopupPanel's child widget, we have to use super.getWidget().
    Widget w = super.getWidget();
    if (w != null) {
      if (desiredHeight != null) {
        w.setHeight(desiredHeight);
      }
      if (desiredWidth != null) {
        w.setWidth(desiredWidth);
      }
    }
  }

  /**
   * Set the type of animation to use when opening and closing the popup.
   *
   * @see AnimationType
   * @param type the type of animation to use
   */
  public void setAnimationType(AnimationType type) {
    animType = type != null ? type : AnimationType.CENTER;
  }

  /**
   * Does the event target one of the partner elements?
   *
   * @param event the native event
   * @return true if the event targets a partner
   */
  private boolean eventTargetsPartner(NativeEvent event) {
    if (autoHidePartners == null) {
      return false;
    }

    EventTarget target = event.getEventTarget();
    if (Element.is(target)) {
      for (Element elem : autoHidePartners) {
        if (elem.isOrHasChild(Element.as(target))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Does the event target this popup?
   *
   * @param event the native event
   * @return true if the event targets the popup
   */
  private boolean eventTargetsPopup(NativeEvent event) {
    EventTarget target = event.getEventTarget();
    if (Element.is(target)) {
      return getElement().isOrHasChild(Element.as(target));
    }
    return false;
  }

  /**
   * Get the element that {@link PopupImpl} uses. PopupImpl creates an element
   * that goes inside of the outer element, so all methods in PopupImpl are
   * relative to the first child of the outer element, not the outer element
   * itself.
   *
   * @return the Element that {@link PopupImpl} creates and expects
   */
  private com.google.gwt.user.client.Element getPopupImplElement() {
    return DOM.getFirstChild(super.getContainerElement());
  }

  /**
   * Positions the popup, called after the offset width and height of the popup
   * are known.
   *
   * @param relativeObject the ui object to position relative to
   * @param offsetWidth the drop down's offset width
   * @param offsetHeight the drop down's offset height
   */
  private void position(final Element relativeObject, int offsetWidth,
      int offsetHeight) {
    // Calculate left position for the popup. The computation for
    // the left position is bidi-sensitive.

    int textBoxOffsetWidth = relativeObject.getOffsetWidth();

    // Compute the difference between the popup's width and the
    // textbox's width
    int offsetWidthDiff = offsetWidth - textBoxOffsetWidth;

    int left;

    if (LocaleInfo.getCurrentLocale().isRTL()) { // RTL case

      int textBoxAbsoluteLeft = relativeObject.getAbsoluteLeft();

      // Right-align the popup. Note that this computation is
      // valid in the case where offsetWidthDiff is negative.
      left = textBoxAbsoluteLeft - offsetWidthDiff;

      // If the suggestion popup is not as wide as the text box, always
      // align to the right edge of the text box. Otherwise, figure out whether
      // to right-align or left-align the popup.
      if (offsetWidthDiff > 0) {

        // Make sure scrolling is taken into account, since
        // box.getAbsoluteLeft() takes scrolling into account.
        int windowRight = Window.getClientWidth() + Window.getScrollLeft();
        int windowLeft = Window.getScrollLeft();

        // Compute the left value for the right edge of the textbox
        int textBoxLeftValForRightEdge = textBoxAbsoluteLeft
            + textBoxOffsetWidth;

        // Distance from the right edge of the text box to the right edge
        // of the window
        int distanceToWindowRight = windowRight - textBoxLeftValForRightEdge;

        // Distance from the right edge of the text box to the left edge of the
        // window
        int distanceFromWindowLeft = textBoxLeftValForRightEdge - windowLeft;

        // If there is not enough space for the overflow of the popup's
        // width to the right of the text box and there IS enough space for the
        // overflow to the right of the text box, then left-align the popup.
        // However, if there is not enough space on either side, stick with
        // right-alignment.
        if (distanceFromWindowLeft < offsetWidth
            && distanceToWindowRight >= offsetWidthDiff) {
          // Align with the left edge of the text box.
          left = textBoxAbsoluteLeft;
        }
      }
    } else { // LTR case

      // Left-align the popup.
      left = relativeObject.getAbsoluteLeft();

      // If the suggestion popup is not as wide as the text box, always align to
      // the left edge of the text box. Otherwise, figure out whether to
      // left-align or right-align the popup.
      if (offsetWidthDiff > 0) {
        // Make sure scrolling is taken into account, since
        // box.getAbsoluteLeft() takes scrolling into account.
        int windowRight = Window.getClientWidth() + Window.getScrollLeft();
        int windowLeft = Window.getScrollLeft();

        // Distance from the left edge of the text box to the right edge
        // of the window
        int distanceToWindowRight = windowRight - left;

        // Distance from the left edge of the text box to the left edge of the
        // window
        int distanceFromWindowLeft = left - windowLeft;

        // If there is not enough space for the overflow of the popup's
        // width to the right of hte text box, and there IS enough space for the
        // overflow to the left of the text box, then right-align the popup.
        // However, if there is not enough space on either side, then stick with
        // left-alignment.
        if (distanceToWindowRight < offsetWidth
            && distanceFromWindowLeft >= offsetWidthDiff) {
          // Align with the right edge of the text box.
          left -= offsetWidthDiff;
        }
      }
    }

    // Calculate top position for the popup

    int top = relativeObject.getAbsoluteTop();

    // Make sure scrolling is taken into account, since
    // box.getAbsoluteTop() takes scrolling into account.
    int windowTop = Window.getScrollTop();
    int windowBottom = Window.getScrollTop() + Window.getClientHeight();

    // Distance from the top edge of the window to the top edge of the
    // text box
    int distanceFromWindowTop = top - windowTop;

    // Distance from the bottom edge of the window to the bottom edge of
    // the text box
    int distanceToWindowBottom = windowBottom
        - (top + relativeObject.getOffsetHeight());

    // If there is not enough space for the popup's height below the text
    // box and there IS enough space for the popup's height above the text
    // box, then position the popup above the text box. However, if there
    // is not enough space on either side, then stick with displaying the
    // popup below the text box.
    if (distanceToWindowBottom < offsetHeight
        && distanceFromWindowTop >= offsetHeight) {
      top -= offsetHeight;
    } else {
      // Position above the text box
      top += relativeObject.getOffsetHeight();
    }
    setPopupPosition(left, top);
  }

  private void previewNativeEvent(NativePreviewEvent event) {
    // If the event has been canceled or consumed, ignore it
    if (event.isCanceled()) {
      // We need to ensure that we cancel the event even if its been consumed so
      // that popups lower on the stack do not auto hide
      return;
    }

    if (event.isCanceled()) {
      return;
    }

    // If the event targets the popup or the partner, consume it
    Event nativeEvent = Event.as(event.getNativeEvent());

    boolean eventTargetsPopupOrPartner = eventTargetsPopup(nativeEvent)
        || eventTargetsPartner(nativeEvent);
    if (eventTargetsPopupOrPartner) {
      event.consume();
    }

    // Switch on the event type
    int type = nativeEvent.getTypeInt();
    switch (type) {
      case Event.ONKEYDOWN:
      case Event.ONKEYPRESS:
      case Event.ONKEYUP: {
        return;
      }

      case Event.ONMOUSEDOWN:
      case Event.ONTOUCHSTART:
        // Don't eat events if event capture is enabled, as this can
        // interfere with dialog dragging, for example.
        if (DOM.getCaptureElement() != null) {
          event.consume();
          return;
        }

        if (!eventTargetsPopupOrPartner) {
          hide(true);
          return;
        }
        break;
      case Event.ONMOUSEUP:
      case Event.ONMOUSEMOVE:
      case Event.ONCLICK:
      case Event.ONDBLCLICK:
      case Event.ONTOUCHEND: {
        // Don't eat events if event capture is enabled, as this can
        // interfere with dialog dragging, for example.
        if (DOM.getCaptureElement() != null) {
          event.consume();
          return;
        }
        break;
      }

      case Event.ONFOCUS: {
        break;
      }
    }
  }

  /**
   * Register or unregister the handlers used by {@link PopupPanel}.
   */
  private void updateHandlers() {
    // Remove any existing handlers.
    if (nativePreviewHandlerRegistration != null) {
      nativePreviewHandlerRegistration.removeHandler();
      nativePreviewHandlerRegistration = null;
    }
    if (historyHandlerRegistration != null) {
      historyHandlerRegistration.removeHandler();
      historyHandlerRegistration = null;
    }

    // Create handlers if showing.
    if (showing) {
      nativePreviewHandlerRegistration = Event.addNativePreviewHandler(this::previewNativeEvent);
      historyHandlerRegistration = History.addValueChangeHandler(event -> hide());
    }
  }

  private static class DecoratorPanel extends SimplePanel {

    private Element containerElem;

    DecoratorPanel(String[] rowStyles, int containerIndex) {
      super(DOM.createTable());

      // Add a tbody
      Element table = getElement();
      Element tbody = DOM.createTBody();
      DOM.appendChild(table, tbody);
      table.setPropertyInt("cellSpacing", 0);
      table.setPropertyInt("cellPadding", 0);

      // Add each row
      for (int i = 0; i < rowStyles.length; i++) {
        Element row = createTR(rowStyles[i]);
        DOM.appendChild(tbody, row);
        if (i == containerIndex) {
          containerElem = DOM.getFirstChild(DOM.getChild(row, 1));
        }
      }
    }

    Element createTR(String styleName) {
      Element trElem = DOM.createTR();
      setStyleName(trElem, styleName);
      if (LocaleInfo.getCurrentLocale().isRTL()) {
        DOM.appendChild(trElem, createTD(styleName + "Right"));
        DOM.appendChild(trElem, createTD(styleName + "Center"));
        DOM.appendChild(trElem, createTD(styleName + "Left"));
      } else {
        DOM.appendChild(trElem, createTD(styleName + "Left"));
        DOM.appendChild(trElem, createTD(styleName + "Center"));
        DOM.appendChild(trElem, createTD(styleName + "Right"));
      }
      return trElem;
    }

    private Element createTD(String styleName) {
      Element tdElem = DOM.createTD();
      Element inner = DOM.createDiv();
      DOM.appendChild(tdElem, inner);
      setStyleName(tdElem, styleName);
      setStyleName(inner, styleName + "Inner");
      return tdElem;
    }

    @Override
    protected com.google.gwt.user.client.Element getContainerElement() {
      return DOM.asOld(containerElem);
    }
  }
}
