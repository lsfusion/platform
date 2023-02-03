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
package lsfusion.gwt.client.base.view.grid;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.cell.Cell;

public abstract class Column<T, C> implements HasNativeSID {

  public Column() {
  }

  public abstract boolean isFocusable();

  public abstract boolean isSticky();

  public abstract void onEditEvent(EventHandler handler, Cell editCell, Element editRenderElement);

  public abstract void renderDom(Cell cell, TableCellElement cellElement);

  public abstract void updateDom(Cell cell, TableCellElement cellElement);

//  public Element getSizedDom(Cell cell, TableCellElement cellElement) {
//      return cellElement;
//  }
//
  public abstract boolean isCustomRenderer();
}
