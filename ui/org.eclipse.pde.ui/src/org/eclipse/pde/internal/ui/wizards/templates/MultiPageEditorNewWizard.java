/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;

public class MultiPageEditorNewWizard extends NewPluginTemplateWizard {
	private static final String KEY_WTITLE = "MultiPageEditorNewWizard.wtitle"; //$NON-NLS-1$
	private static String fileExtension;
	/**
	 * Constructor for MultiPageEditorNewWizard.
	 */
	public MultiPageEditorNewWizard() {
		super();
	}
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		fileExtension = "mpe"; //$NON-NLS-1$
	}
	
	public void setFileExtension(String ext){
		fileExtension = ext;
	}
	
	public String getFileExtension(){
		if (fileExtension == null || fileExtension.length() == 0)
			return "mpe"; //$NON-NLS-1$
		return fileExtension;
	}

	/*
	 * @see NewExtensionTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection [] {
				new MultiPageEditorTemplate(this),
				new NewWizardTemplate(this) };
	}
}