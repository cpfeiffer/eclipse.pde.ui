/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.PDE;
import org.osgi.framework.Version;

/**
 * Util class for a variety of project related operations
 * 
 * @since 1.0.0
 */
public class ProjectUtils {
	
	/**
	 * Constant representing the name of the output directory for a project.
	 * Value is: <code>bin</code>
	 */
	public static final String BIN_FOLDER = "bin";
	
	/**
	 * Constant representing the name of the source directory for a project.
	 * Value is: <code>src</code>
	 */
	public static final String SRC_FOLDER = "src";
	
	public static IBundleProjectService getBundleProjectService() {
		return (IBundleProjectService) PDECore.getDefault().acquireService(IBundleProjectService.class.getName());
	}
	
	/**
	 * Crate a plug-in project with the given name
	 * @param projectName
	 * @param additionalNatures
	 * @return a new plug-in project
	 * @throws CoreException
	 */
	public static IJavaProject createPluginProject(String projectName, String[] additionalNatures) throws CoreException {
		String[] resolvednatures = additionalNatures;
		if(additionalNatures != null) {
			ArrayList natures = new ArrayList(Arrays.asList(additionalNatures));
			if(!natures.contains(PDE.PLUGIN_NATURE)) {
				//need to always set this one first, in case others depend on it, like API tooling does
				natures.add(0, PDE.PLUGIN_NATURE);
			}
			if (!natures.contains(JavaCore.NATURE_ID)) {
				natures.add(0, JavaCore.NATURE_ID);
			}
			resolvednatures = (String[]) natures.toArray(new String[natures.size()]);
		}
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		IBundleClasspathEntry entry = service.newBundleClasspathEntry(new Path(SRC_FOLDER), new Path(BIN_FOLDER), null);
		description.setSymbolicName(projectName);
		description.setBundleClassath(new IBundleClasspathEntry[]{entry});
		description.setNatureIds(resolvednatures);
		description.setBundleVendor("ibm");
		description.setTargetVersion(IBundleProjectDescription.VERSION_3_4);
		description.setEqunioxHeaders(true);
		description.setBundleVersion(new Version("1.0.0"));
		description.apply(null);		
		return JavaCore.create(project);
	}
	
	/**
	 * creates a java project with the specified name and additional project natures
	 * @param projectName
	 * @param additionalNatures
	 * @return a new java project
	 * @throws CoreException
	 */
	public static IJavaProject createJavaProject(String projectName, String[] additionalNatures) throws CoreException {
		IProgressMonitor monitor = new NullProgressMonitor();
		IProject project = createProject(projectName, monitor);
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID, monitor);
		}
		if(additionalNatures != null) {
			for(int i = 0; i < additionalNatures.length; i++) {
				addNatureToProject(project, additionalNatures[i], monitor);
			}
		}
		IJavaProject jproject = JavaCore.create(project);
		jproject.setOutputLocation(getDefaultProjectOutputLocation(project), monitor);
		jproject.setRawClasspath(new IClasspathEntry[0], monitor);
		addContainerEntry(jproject, JavaRuntime.newDefaultJREContainerPath());
		return jproject;	
	}
	
	/**
	 * Gets the output location for the given project, creates it if needed
	 * @param project
	 * @return the path of the output location for the given project
	 * @throws CoreException
	 */
	public static IPath getDefaultProjectOutputLocation(IProject project) throws CoreException {
		IFolder binFolder = project.getFolder(BIN_FOLDER);
		if (!binFolder.exists()) {
			binFolder.create(false, true, null);
		}
		return binFolder.getFullPath();
	}
	
	/**
	 * Adds a new source container specified by the container name to the source path of the specified project
	 * @param jproject
	 * @param containerName
	 * @return the package fragment root of the container name
	 * @throws CoreException
	 */
	public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName) throws CoreException {
		IProject project = jproject.getProject();
		IPackageFragmentRoot root = jproject.getPackageFragmentRoot(addFolderToProject(project, containerName));
		IClasspathEntry cpe = JavaCore.newSourceEntry(root.getPath());
		addToClasspath(jproject, cpe);
		return root;
	}  
	
	/**
	 * Adds a container entry to the specified java project
	 * @param project
	 * @param container
	 * @throws JavaModelException
	 */
	public static void addContainerEntry(IJavaProject project, IPath container) throws JavaModelException {
		IClasspathEntry cpe = JavaCore.newContainerEntry(container, false);
		addToClasspath(project, cpe);
	}
	
	/**
	 * Adds a folder with the given name to the specified project
	 * @param project
	 * @param name
	 * @return the new container added to the specified project
	 * @throws CoreException
	 */
	public static IContainer addFolderToProject(IProject project, String name) throws CoreException {
		IContainer container= null;
		if (name == null || name.length() == 0) {
			container = project;
		} else {
			IFolder folder = project.getFolder(name);
			if (!folder.exists()) {
				folder.create(false, true, null);
			}
			container = folder;
		}
		return container;
	}
	
	/**
	 * Adds the specified classpath entry to the specified java project
	 * @param jproject
	 * @param cpe
	 * @throws JavaModelException
	 */
	public static void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
		boolean found = false;
		IClasspathEntry[] entries = jproject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			if (entriesEqual(entries[i], cpe)) {
				entries[i] = cpe;
				found = true;
			}
		}
		if(!found) {
			int nEntries = entries.length;
			IClasspathEntry[] newEntries = new IClasspathEntry[nEntries + 1];
			System.arraycopy(entries, 0, newEntries, 0, nEntries);
			newEntries[nEntries] = cpe;
			entries = newEntries;
		}
		jproject.setRawClasspath(entries, true, new NullProgressMonitor());
	}  
	
	/**
	 * Removes the specified entry from the classpath of the specified project
	 * @param project
	 * @param entry
	 * @throws JavaModelException
	 */
	public static void removeFromClasspath(IJavaProject project, IClasspathEntry entry) throws JavaModelException {
		IClasspathEntry[] oldEntries = project.getRawClasspath();
		ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		for (int i= 0; i < oldEntries.length; i++) {
			if (!oldEntries[i].equals(entry)) {
				entries.add(oldEntries[i]);
			}
		}
		if(entries.size() != oldEntries.length) {
			project.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), new NullProgressMonitor());
		}
	}
	
	/**
	 * Delegate equals method to cover the test cases where we want to insert an updated element 
	 * and one with the same path/type/kind is already there. 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private static boolean entriesEqual(IClasspathEntry e1, IClasspathEntry e2) {
		return e1.equals(e2) || (e1.getEntryKind() == e2.getEntryKind() && e1.getContentKind() == e2.getContentKind() && e1.getPath().equals(e2.getPath()));
	}
	
	/**
	 * Creates a project with the given name in the workspace and returns it.
	 * If a project with the given name exists, it is refreshed and opened (if closed) and returned
	 * @param projectName
	 * @param monitor
	 * @return a project with the given name
	 * @throws CoreException
	 */
	public static IProject createProject(String projectName, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (!project.exists()) {
			project.create(monitor);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		if (!project.isOpen()) {
			project.open(monitor);
		}
		return project;
	}
	
	/**
	 * Adds the specified nature to the specified project
	 * @param proj
	 * @param natureId
	 * @param monitor
	 * @throws CoreException
	 */
	public static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
	/**
	 * Removes the given package from the exported packages header, if it exists.
	 * 
	 * This method is not safe to use in a headless manner.
	 * 
	 * @param project the project to remove the package from
	 * @param packagename the name of the package to remove from the export package header
	 */
	public static void removeExportedPackage(IProject project, String packagename) throws CoreException {
		IBundleProjectDescription description = getBundleProjectService().getDescription(project);
		IPackageExportDescription[] exports = description.getPackageExports();
		if (exports != null) {
			List list = new ArrayList();
			for (int i = 0; i < exports.length; i++) {
				if (!packagename.equals(exports[i].getName())) {
					list.add(exports[i]);
				}
			}
			if (list.size() < exports.length) {
				description.setPackageExports((IPackageExportDescription[]) list.toArray(new IPackageExportDescription[list.size()]));
				description.apply(null);
			}
		}
	}
	
	/**
	 * Adds a new exported package to the manifest.
	 * 
	 * This method is not safe to use in a headless manner.
	 * 
	 * @param project the project to get the manifest information from
	 * @param packagename the fully qualified name of the package to add 
	 * @param internal if the added package should be internal or not
	 * @param friends a listing of friends for this exported package
	 * @throws CoreException if something bad happens
	 */
	public static void addExportedPackage(IProject project, String packagename, boolean internal, String[] friends) throws CoreException {
		if(!project.exists() || packagename == null) {
			//do no work
			return;
		}
		IBundleProjectService service = getBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		IPackageExportDescription[] exports = description.getPackageExports();
		List list = new ArrayList();
		if (exports != null) {
			for (int i = 0; i < exports.length; i++) {
				list.add(exports[i]);
			}
		}
		list.add(service.newPackageExport(packagename, null, !internal, friends));
		description.setPackageExports((IPackageExportDescription[]) list.toArray(new IPackageExportDescription[list.size()]));
		description.apply(null);
	}
	
	/**
	 * Returns the {@link IPackageExportDescription}s for the given project or <code>null</code>
	 * if none.
	 * 
	 * @param project the proejct
	 * @return the {@link IPackageExportDescription}s for the given project or <code>null</code>
	 */
	public static IPackageExportDescription[] getExportedPackages(IProject project) throws CoreException {
		IBundleProjectDescription description = getBundleProjectService().getDescription(project);
		return description.getPackageExports();
	}
}
