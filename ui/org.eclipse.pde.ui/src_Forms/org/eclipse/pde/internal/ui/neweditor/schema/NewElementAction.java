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
package org.eclipse.pde.internal.ui.neweditor.schema;

import java.util.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;

public class NewElementAction extends Action {
	private Schema schema;
	private static final String NAME_COUNTER_KEY = "__schema_element_name";
	public static final String KEY_LABEL = "SchemaEditor.NewElement.label";
	public static final String KEY_TOOLTIP = "SchemaEditor.NewElement.tooltip";
	public static final String KEY_INITIAL_NAME =
		"SchemaEditor.NewElement.initialName";

	public NewElementAction() {
		setText(PDEPlugin.getResourceString(KEY_LABEL));
		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
		setToolTipText(PDEPlugin.getResourceString(KEY_TOOLTIP));
	}
	private String getInitialName() {
		Hashtable counters = PDEPlugin.getDefault().getDefaultNameCounters();
		Integer counter = (Integer) counters.get(NAME_COUNTER_KEY);
		if (counter == null) {
			counter = new Integer(1);
		} else {
			counter = new Integer(counter.intValue() + 1);
		}
		counters.put(NAME_COUNTER_KEY, counter);
		return PDEPlugin.getFormattedMessage(
			KEY_INITIAL_NAME,
			counter.intValue() + "");
	}
	public org.eclipse.pde.internal.core.schema.Schema getSchema() {
		return schema;
	}
	public void run() {
		String name = getInitialName();
		SchemaElement element = new SchemaElement(schema, name);
		element.setType(new SchemaSimpleType(schema, "string"));
		schema.addElement(element);
		schema.updateReferencesFor(element, Schema.REFRESH_ADD);
	}
	public void setSchema(Schema newSchema) {
		schema = newSchema;
	}
}
