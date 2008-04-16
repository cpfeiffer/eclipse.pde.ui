/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.internal.ds.core.text.DSObject;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class DSFormOutlinePage extends FormOutlinePage  {

	public DSFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}
	
	public class DSLabelProvider extends BasicLabelProvider {
		public String getText(Object obj) {
			if (obj instanceof DSObject) {
				return getObjectText((DSObject) obj);
			}
			return super.getText(obj);
		}

	}
	

	protected String getObjectText(DSObject obj) {
		//TODO verify limits?
		return obj.getName();
	}
}