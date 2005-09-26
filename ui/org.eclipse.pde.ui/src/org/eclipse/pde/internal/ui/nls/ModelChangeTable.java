package org.eclipse.pde.internal.ui.nls;

import java.util.Collection;
import java.util.Hashtable;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ModelChangeTable {

	private Hashtable fChangeTable = new Hashtable();
	
	public void addToChangeTable(IPluginModelBase model, IFile file, Object change, boolean selected) {
		if (change == null) return;
		ModelChange modelChange;
		if (fChangeTable.containsKey(model))
			modelChange = (ModelChange)fChangeTable.get(model);
		else {
			modelChange = new ModelChange(model, selected);
			fChangeTable.put(model, modelChange);
		}
		modelChange.addChange(file, new ModelChangeElement(modelChange, change));
	}
	
	public Collection getAllModelChanges() {
		return fChangeTable.values();
	}
	
	public ModelChange getModelChange(IPluginModelBase modelKey) {
		if (fChangeTable.containsKey(modelKey))
			return (ModelChange)fChangeTable.get(modelKey);
		return null;
	}
}
