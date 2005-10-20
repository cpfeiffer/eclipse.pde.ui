/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.pde.internal.core.ibundle.IBundle;

public class ImportPackageHeader extends BasePackageHeader {

    private static final long serialVersionUID = 1L;

    public ImportPackageHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

    protected void processValue(String value) {
       for (int i = 0; i < fManifestElements.size(); i++) {
			ImportPackageObject p = 
				new ImportPackageObject(this, fManifestElements.get(i), getVersionAttribute());
			fPackages.put(p.getName(), p);
       }
    }
    
    public ImportPackageObject[] getPackages() {
        return (ImportPackageObject[])fPackages.values().toArray(new ImportPackageObject[fPackages.size()]);
    }
    


}
