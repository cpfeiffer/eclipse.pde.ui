/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.editor;

import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.pde.internal.base.model.IModel;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @version 	1.0
 * @author
 */
public abstract class TableSection extends StructuredViewerSection {
	class PartAdapter extends EditableTablePart {
		public PartAdapter(String [] buttonLabels) {
			super(buttonLabels);
		}
		public void entryModified(Object entry, String value) {
			TableSection.this.entryModified(entry, value);
		}
		public void selectionChanged(IStructuredSelection selection) {
			TableSection.this.selectionChanged(selection);
		}
		public void handleDoubleClick(IStructuredSelection selection) {
			TableSection.this.handleDoubleClick(selection);
		}
		public void buttonSelected(Button button, int index) {
			TableSection.this.buttonSelected(index);
			button.getShell().setDefaultButton(null);
		}
	}
	/**
	 * Constructor for TableSection.
	 * @param formPage
	 */
	public TableSection(PDEFormPage formPage, String [] buttonLabels) {
		super(formPage, buttonLabels);
	}

	protected StructuredViewerPart createViewerPart(String [] buttonLabels) {
		IModel model = (IModel)getFormPage().getModel();
		EditableTablePart tablePart;
		tablePart = new PartAdapter(buttonLabels);
		tablePart.setEditable(model.isEditable());
		return tablePart;
	}
	
	protected TablePart getTablePart() {
		return (TablePart)viewerPart;
	}
	
	protected void entryModified(Object entry, String value) {
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
	}
}