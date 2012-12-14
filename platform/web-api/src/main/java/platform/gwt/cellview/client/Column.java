/*
 * Copyright 2010 Google Inc.
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
package platform.gwt.cellview.client;

import com.google.gwt.user.client.ui.HasAlignment;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.cellview.client.cell.HasCell;

/**
 * A representation of a column in a table.
 * 
 * @param <T> the row type
 * @param <C> the column type
 */
public abstract class Column<T, C> implements HasCell<T, C>, HasAlignment {

  /**
   * The {@link platform.gwt.cellview.client.cell.Cell} responsible for rendering items in the column.
   */
  private final Cell<C> cell;

  private HorizontalAlignmentConstant hAlign = null;
  private VerticalAlignmentConstant vAlign = null;

  /**
   * Construct a new Column with a given {@link Cell}.
   * 
   * @param cell the Cell used by this Column
   */
  public Column(Cell<C> cell) {
    this.cell = cell;
  }

  /**
   * Returns the {@link Cell} responsible for rendering items in the column.
   * 
   * @return a Cell
   */
  @Override
  public Cell<C> getCell() {
    return cell;
  }

  @Override
  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return hAlign;
  }
  
  /**
   * Returns the column value from within the underlying data object.
   */
  @Override
  public abstract C getValue(T object);

  @Override
  public VerticalAlignmentConstant getVerticalAlignment() {
    return vAlign;
  }

  /**
   * {@inheritDoc}
   * 
   * <p>
   * The new horizontal alignment will apply the next time the table is
   * rendered.
   * </p>
   */
  @Override
  public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
    this.hAlign = align;
  }

  /**
   * {@inheritDoc}
   * 
   * <p>
   * The new vertical alignment will apply the next time the table is rendered.
   * </p>
   */
  @Override
  public void setVerticalAlignment(VerticalAlignmentConstant align) {
    this.vAlign = align;
  }
}
