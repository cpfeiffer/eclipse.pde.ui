/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;

/**
 * This Operation is used to find possible resolutions to an unresolved class reference in a plug-in project.
 * When it is run, it will pass any ExportPackageDescriptions which provide the package to the AbstractClassResolutionCollector.
 * The AbstractClassResolutionCollector is responsible for creating the appropriate resolutions.
 *
 * @since 3.4
 */
public class FindClassResolutionsOperation implements IRunnableWithProgress {

	String fClassName = null;
	IProject fProject = null;
	AbstractClassResolutionCollector fCollector = null;

	/**
	 * This class is meant to be sub-classed for use with FindClassResolutionsOperation.  The subclass is responsible for creating 
	 * corresponding proposals with the help of JavaResolutionFactory.
	 * 
	 * @since 3.4
	 * @see JavaResolutionFactory
	 */
	public static abstract class AbstractClassResolutionCollector {

		/**
		 * This method is meant to be sub-classed.  The subclass should decide if it wishes to create a proposals for either
		 * Require-Bundle and/or Import-Package.  The proposals can be created with the help of the JavaResolutionFactory
		 */
		abstract public void addResolutionModification(IProject project, ExportPackageDescription desc);

		/**
		 * Adds an export package proposal. Subclasses should implement the actual adding to the collection.
		 */
		public Object addExportPackageResolutionModification(IPackageFragment aPackage) {
			if (aPackage.exists()) {
				return JavaResolutionFactory.createExportPackageProposal(aPackage.getResource().getProject(), aPackage, JavaResolutionFactory.TYPE_JAVA_COMPLETION, 100);
			}
			return null;
		}

		/*
		 * Optimization for case where users is only interested in Import-Package and therefore can quit after first dependency is found
		 */
		public boolean isDone() {
			return false;
		}

	}

	/**
	 * This class is used to try to find resolutions to unresolved java classes.  When either an Import-Package or Require-Bundle might 
	 * resolve a class, the ExportPackageDescription which contains the package/bundle will be passed to the AbstractClassResoltuionCollector.
	 * The collector is then responsible for creating an corresponding resolutions with the help of JavaResolutionFactory.
	 * @param project the project which contains the unresolved class
	 * @param className	the name of the class which is unresolved
	 * @param collector a subclass of AbstractClassResolutionCollector to collect/handle possible resolutions 
	 */
	public FindClassResolutionsOperation(IProject project, String className, AbstractClassResolutionCollector collector) {
		fProject = project;
		fClassName = className;
		fCollector = collector;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) {
		int idx = fClassName.lastIndexOf('.');
		String packageName = idx != -1 ? fClassName.substring(0, idx) : null;
		String typeName = fClassName.substring(idx + 1);
		if (typeName.length() == 1 && typeName.charAt(0) == '*') {
			typeName = null;
		}

		Set packagesToExport = new HashSet();
		List validPackages = getValidPackages(typeName, packageName, packagesToExport, monitor);
		if (validPackages != null) {
			Iterator validPackagesIter = validPackages.iterator();
			Set visiblePkgs = null;

			if (validPackages.isEmpty()) {
				for (Iterator it = packagesToExport.iterator(); it.hasNext();) {
					IPackageFragment packageFragment = (IPackageFragment) it.next();
					fCollector.addExportPackageResolutionModification(packageFragment);
				}
				return;
			}

			while (validPackagesIter.hasNext() && !fCollector.isDone()) {
				// since getting visible packages is not very efficient, only do it once and cache result
				if (visiblePkgs == null) {
					visiblePkgs = getVisiblePackages();
				}
				ExportPackageDescription currentPackage = (ExportPackageDescription) validPackagesIter.next();
				// if package is already visible, skip over
				if (visiblePkgs.contains(currentPackage)) {
					continue;
				}
				// if currentPackage will resolve class and is valid, pass it to collector
				fCollector.addResolutionModification(fProject, currentPackage);
			}
		}
	}

	private List getValidPackages(String typeName, String packageName, Set packagesToExport, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 3);

		List validPackages = null;
		ImportPackageSpecification[] importPkgs = null;
		IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
		if (model != null && model.getBundleDescription() != null) {
			importPkgs = model.getBundleDescription().getImportPackages();
		}
		subMonitor.worked(1);

		if (importPkgs != null) {
			if (packageName != null) {
				if (!isImportedPackage(packageName, importPkgs)) {
					validPackages = new ArrayList(getValidPackages(packageName));
				}
				subMonitor.worked(1);
			} else {
				// find possible types in the global packages
				validPackages = new ArrayList(findValidPackagesContainingSimpleType(typeName, importPkgs, packagesToExport, subMonitor.newChild(1)));
			}
		}
		return validPackages;
	}

	/**
	 * Finds all exported packages containing the simple type aTypeName. The packages
	 * will be filtered from the given packages which are already imported, and all 
	 * system packages.
	 * 
	 * If no exported package is left, packagesToExport will be filled with those
	 * packages that would have been returned, if they were exported.  
	 * @param aTypeName the simple type to search for
	 * @param importPkgs the packages which are already imported
	 * @param packagesToExport return parameter that will be filled with packages to export
	 * 		 if no valid package to import was found
	 * @param monitor
	 * @return the set of packages to import
	 */
	private Set findValidPackagesContainingSimpleType(String aTypeName, ImportPackageSpecification[] importPkgs, Set packagesToExport, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);

		IPluginModelBase[] activeModels = PluginRegistry.getActiveModels();
		Set projects = new HashSet(activeModels.length * 2);

		for (int i = 0; i < activeModels.length; i++) {
			IResource resource = activeModels[i].getUnderlyingResource();
			if (resource != null && resource.isAccessible()) {
				IJavaProject javaProject = JavaCore.create(resource.getProject());
				if (javaProject.exists()) {
					projects.add(javaProject);
				}
			}
		}

		try {
			IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope((IJavaElement[]) projects.toArray(new IJavaElement[projects.size()]));

			final Map packages = new HashMap();
			SearchRequestor requestor = new SearchRequestor() {

				public void acceptSearchMatch(SearchMatch aMatch) throws CoreException {
					Object element = aMatch.getElement();
					if (element instanceof IType) {
						IType type = (IType) element;
						IPackageFragment packageFragment = type.getPackageFragment();
						if (packageFragment.exists()) {
							packages.put(packageFragment.getElementName(), packageFragment);
						}
					}
				}
			};

			SearchPattern typePattern = SearchPattern.createPattern(aTypeName, IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH);
			new SearchEngine().search(typePattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, searchScope, requestor, subMonitor.newChild(1));

			if (!packages.isEmpty()) {
				// transform to ExportPackageDescriptions
				Set exportDescriptions = new HashSet(packages.size());

				// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
				ExportPackageDescription[] systemPackages = PDECore.getDefault().getModelManager().getState().getState().getSystemPackages();
				for (int i = 0; i < systemPackages.length; i++) {
					packages.remove(systemPackages[i].getName());
				}
				// also remove packages that are already imported
				for (int i = 0; i < importPkgs.length; i++) {
					packages.remove(importPkgs[i].getName());
				}

				// finally create the list of ExportPackageDescriptions
				ExportPackageDescription[] knownPackages = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
				for (int i = 0; i < knownPackages.length; i++) {
					if (packages.containsKey(knownPackages[i].getName())) {
						exportDescriptions.add(knownPackages[i]);
					}
				}
				if (exportDescriptions.isEmpty()) {
					// no packages to import found, maybe there are packages to export
					packagesToExport.addAll(packages.values());
				}

				return exportDescriptions;
			}

			return Collections.EMPTY_SET;
		} catch (CoreException ex) {
			// ignore, return an empty set
			return Collections.EMPTY_SET;
		}
	}

	private boolean isImportedPackage(String packageName, ImportPackageSpecification[] importPkgs) {
		for (int i = 0; i < importPkgs.length; i++) {
			if (importPkgs[i].getName().equals(packageName)) {
				return true;
			}
		}
		return false;
	}

	private static Set getValidPackages(String pkgName) {
		ExportPackageDescription[] knownPackages = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
		Set validPackages = new HashSet();
		for (int i = 0; i < knownPackages.length; i++) {
			if (knownPackages[i].getName().equals(pkgName)) {
				validPackages.add(knownPackages[i]);
			}
		}
		// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
		if (!validPackages.isEmpty()) {
			knownPackages = PDECore.getDefault().getModelManager().getState().getState().getSystemPackages();
			for (int i = 0; i < knownPackages.length; i++) {
				validPackages.remove(knownPackages[i]);
			}
		}
		return validPackages;
	}

	private Set getVisiblePackages() {
		IPluginModelBase base = PluginRegistry.findModel(fProject);
		BundleDescription desc = base.getBundleDescription();

		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		ExportPackageDescription[] visiblePkgs = helper.getVisiblePackages(desc);

		HashSet set = new HashSet();
		for (int i = 0; i < visiblePkgs.length; i++) {
			set.add(visiblePkgs[i]);
		}
		return set;
	}

}
