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
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
/**
 * @version 	1.0
 * @author
 */
public interface IFeatureImport extends IFeatureObject, IPluginReference, IEnvironment {
	String P_TYPE = "type";
	String P_PATCH = "patch";
	String P_ID_MATCH = "id-match";
	
	int PLUGIN = 0;
	int FEATURE = 1;
	
	int getType();
	
	void setType(int type) throws CoreException;
	
	boolean isPatch();
	void setPatch(boolean patch) throws CoreException;
	
	int getIdMatch();
	void setIdMatch(int idMatch) throws CoreException;
}
