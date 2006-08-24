/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.cheatsheet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * CheatSheetFileWizardPage
 *
 */
public class CheatSheetFileWizardPage extends WizardNewFileCreationPage {

	private Button fSimpleCheatSheetButton;
	
	private Button fCompositeCheatSheetButton;
	
	private Group fGroup;
	
	private static final String F_FILE_EXTENSION = ".xml"; //$NON-NLS-1$
	
	private boolean fHasExtension;
	
	public static final int F_SIMPLE_CHEAT_SHEET = 0;
	
	public static final int F_COMPOSITE_CHEAT_SHEET = 1;
	
	/**
	 * @param pageName
	 * @param selection
	 */
	public CheatSheetFileWizardPage(String pageName,
			IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.CheatSheetFileWizardPage_1);
		setDescription(PDEUIMessages.CheatSheetFileWizardPage_2);
		fHasExtension = false;
	}

	/**
	 * @return
	 */
	public int getCheatSheetType() {
		if (fSimpleCheatSheetButton.getSelection()) {
			return F_SIMPLE_CHEAT_SHEET;
		} else if (fCompositeCheatSheetButton.getSelection()) {
			return F_COMPOSITE_CHEAT_SHEET;
		}
		// Neither selected. Unknown type
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
	 */
	protected void createAdvancedControls(Composite parent) {

		// Cheat Sheet Group
		fGroup = new Group(parent, SWT.NONE);
		fGroup.setText(PDEUIMessages.CheatSheetFileWizardPage_4); 
		fGroup.setLayout(new GridLayout(1, false));
		fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		
		// Simple Cheat Sheet Button
		fSimpleCheatSheetButton = new Button(fGroup, SWT.RADIO);
		fSimpleCheatSheetButton.setText(PDEUIMessages.CheatSheetFileWizardPage_5); 
		fSimpleCheatSheetButton.setSelection(true);
		fSimpleCheatSheetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                getWizard().getContainer().updateButtons();
			}
		});
		
		// Simple Cheat Sheet Description Label
		final Label simpleCSText = new Label(fGroup, SWT.WRAP);
		simpleCSText.setText(PDEUIMessages.CheatSheetFileWizardPage_6);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		simpleCSText.setLayoutData(gd);
		
		// Spacer
		new Label(fGroup, SWT.NULL);
		
		// Composite Cheat Sheet Button
		fCompositeCheatSheetButton = new Button(fGroup, SWT.RADIO);
		fCompositeCheatSheetButton.setSelection(false);
		fCompositeCheatSheetButton
				.setText(PDEUIMessages.CheatSheetFileWizardPage_7); 
		fCompositeCheatSheetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                getWizard().getContainer().updateButtons();
			}
		});		

		// Composite Cheat Sheet Description Label
		final Label compositeCSText = new Label(fGroup, SWT.WRAP);
		compositeCSText.setText(PDEUIMessages.CheatSheetFileWizardPage_8);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		compositeCSText.setLayoutData(gd);
	}

	/**
	 * 
	 */
	public void finalizePage() {
		// If an extension has not been provided, append an '.xml' extension
		if (fHasExtension == false) {
			setFileName(getFileName() + F_FILE_EXTENSION);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
	 */
	protected boolean validatePage() {
		String filename = getFileName().trim();
		// Verify the filename is non-empty
		if (filename.length() == 0) {
			return false;
		}
		
		// Check to see if the filename contains a '.' 
		int dotIndex = filename.indexOf('.');
		if (dotIndex == -1) {
			// Filename contains no dot
			fHasExtension = false;
			return super.validatePage();
		}
		// Filename contains a dot
		fHasExtension = true;
		String name = filename.substring(0, dotIndex);
		// Verify that the name portion is non-empty
		if (name.length() == 0) {
			setErrorMessage(PDEUIMessages.CheatSheetFileWizardPage_9);
			return false;
		}
		String extension = filename.substring(dotIndex + 1, filename.length());
		// Verify that the extension portion is non-empty
		if (extension.length() == 0) {
			setErrorMessage(PDEUIMessages.CheatSheetFileWizardPage_0);
			return false;
		}
		
		return super.validatePage();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK,
				"", null); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(fGroup);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
	 */
	protected void createLinkTarget() {
		// NOOP
	}
}
