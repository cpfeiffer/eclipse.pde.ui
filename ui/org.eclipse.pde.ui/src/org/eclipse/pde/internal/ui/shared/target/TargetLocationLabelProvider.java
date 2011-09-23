/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.pde.core.target.ITargetLocation;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the tree, primary input is a ITargetDefinition, children are ITargetLocation
 */
public class TargetLocationLabelProvider extends StyledBundleLabelProvider {

	public TargetLocationLabelProvider(boolean showVersion, boolean appendResolvedVariables) {
		super(showVersion, appendResolvedVariables);
	}

	Map fLabelProviderMap;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof ITargetLocation) {
			ILabelProvider provider = getLabelProvider((ITargetLocation) element);
			if (provider instanceof StyledCellLabelProvider) {
				((StyledCellLabelProvider) provider).update(cell);
			}
			cell.setText(getText(element));
			cell.setImage(getImage(element));
		} else {
			super.update(cell);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof AbstractBundleContainer) {
			return super.getImage(element);
		} else if (element instanceof ITargetLocation) {
			ILabelProvider provider = getLabelProvider((ITargetLocation) element);
			if (provider != null) {
				return provider.getImage(element);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof AbstractBundleContainer) {
			return super.getText(element);
		} else if (element instanceof ITargetLocation) {
			ILabelProvider provider = getLabelProvider((ITargetLocation) element);
			if (provider != null) {
				return provider.getText(element);
			}
		}
		return null;
	}

	private ILabelProvider getLabelProvider(ITargetLocation location) {
		if (fLabelProviderMap == null) {
			fLabelProviderMap = new HashMap(4);
		}
		ILabelProvider provider = (ILabelProvider) fLabelProviderMap.get(location);
		if (provider == null) {
			provider = (ILabelProvider) Platform.getAdapterManager().getAdapter(location.getType(), ILabelProvider.class);
			fLabelProviderMap.put(location.getType(), provider);
		}
		return provider;
	}
}