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
package org.eclipse.pde.api.tools.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of an API description for a Java project.
 * 
 * @since 1.0
 */
public class ProjectApiDescription extends ApiDescription {
		
	/**
	 * Associated API component.
	 */
	private IJavaProject fProject;
	
	/**
	 * Only valid when connected to a bundle. Used to initialize
	 * package exports.
	 */
	private BundleDescription fBundle;
	
	/**
	 * Time stamp at which package information was created
	 */
	long fPackageTimeStamp;
	
	/** 
	 * Whether a package refresh is in progress
	 */
	private boolean fRefreshingInProgress = false;
	
	/**
	 * Associated manifest file
	 */
	IFile fManifestFile;
	
	/**
	 * Class file container cache used for tag scanning.
	 * Maps package fragment roots to containers.
	 * 
	 * TODO: these could become out of date with class path changes.
	 */
	private Map fClassFileContainers;
			
	/**
	 * A node for a package.
	 */
	class PackageNode extends ManifestNode {

		private IPackageFragment fFragment;
		/**
		 * Constructs a new node.
		 * 
		 * @param parent
		 * @param element
		 * @param visibility
		 * @param restrictions
		 */
		public PackageNode(IPackageFragment pkg, ManifestNode parent, IElementDescriptor element, int visibility, int restrictions) {
			super(parent, element, visibility, restrictions);
			fFragment = pkg;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#refresh()
		 */
		protected ManifestNode refresh() {
			refreshPackages();
			if (fFragment.exists()) {
				return this;
			} else {
				modified();
				return null;
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#persistXML(org.w3c.dom.Document, org.w3c.dom.Element, java.lang.String)
		 */
		void persistXML(Document document, Element parent, String component) {
			Element pkg = document.createElement(ApiDescriptionProcessor.ELEMENT_PACKAGE);
			pkg.setAttribute(ApiDescriptionManager.ATTR_HANDLE, fFragment.getHandleIdentifier());
			persistAnnotations(pkg, component);
			persistChildren(document, pkg, children);
			parent.appendChild(pkg);
		}		
	}
	
	/**
	 * Node for a reference type.
	 */
	class TypeNode extends ManifestNode {
		
		long fTimeStamp = -1L;
		
		private boolean fRefreshing = false;
		
		private IType fType;

		/**
		 * Constructs a node for a reference type.
		 * 
		 * @param type
		 * @param parent
		 * @param element
		 * @param visibility
		 * @param restrictions
		 */
		public TypeNode(IType type, ManifestNode parent, IElementDescriptor element, int visibility, int restrictions) {
			super(parent, element, visibility, restrictions);
			fType = type;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#refresh()
		 */
		protected synchronized ManifestNode refresh() {
			if (fRefreshing) {
				return this;
			}
			try {
				fRefreshing = true;
				if (fType.exists()) {
					ICompilationUnit unit = fType.getCompilationUnit();
					if (unit != null) {
						IResource resource = unit.getCorrespondingResource();
						if (resource != null) {
							long stamp = resource.getModificationStamp();
							if (stamp != fTimeStamp) {
								modified();
								children.clear();
								restrictions = RestrictionModifiers.NO_RESTRICTIONS;
								TagScanner.newScanner().scan(new CompilationUnit(unit), ProjectApiDescription.this,
										getClassFileContainer((IPackageFragmentRoot) fType.getPackageFragment().getParent()));
								fTimeStamp = resource.getModificationStamp();
							}
						} else {
							modified();
							// error - no associated file
							return null;
						}
					} else {
						// TODO: binary type
					}
				} else {
					modified();
					// no longer exists
					parent.children.remove(getElement());
					return null;
				}
			} catch (JavaModelException e) {
				ApiPlugin.log(e.getStatus());
			} catch (FileNotFoundException e) {
				ApiPlugin.log(e);
			} catch (IOException e) {
				ApiPlugin.log(e);
			} finally {
				fRefreshing = false;
			}
			return this;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#persistXML(org.w3c.dom.Document, org.w3c.dom.Element, java.lang.String)
		 */
		void persistXML(Document document, Element parent, String component) {
			Element type = document.createElement(ApiDescriptionProcessor.ELEMENT_TYPE);
			type.setAttribute(ApiDescriptionManager.ATTR_HANDLE, fType.getHandleIdentifier());
			persistAnnotations(type, component);
			type.setAttribute(ApiDescriptionManager.ATTR_MODIFICATION_STAMP, Long.toString(fTimeStamp));
			persistChildren(document, type, children);
			parent.appendChild(type);
		}		
	}
	
	/**
	 * Constructs a new API description for the given Java project.
	 * 
	 * @param component
	 */
	public ProjectApiDescription(IJavaProject project) {
		super(project.getElementName());
		fProject = project;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiDescription#accept(org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor)
	 */
	public void accept(ApiDescriptionVisitor visitor) {
		try {
			IPackageFragment[] fragments = getLocalPackageFragments();
			for (int j = 0; j < fragments.length; j++) {
				IPackageFragment fragment = fragments[j];
				if (DEBUG) {
					System.out.println("\t" + fragment.getElementName().toString()); //$NON-NLS-1$
				}
				IPackageDescriptor packageDescriptor = Factory.packageDescriptor(fragment.getElementName());
				// visit package
				IApiAnnotations annotations = resolveAnnotations(null, packageDescriptor);
				boolean visitChildren  = visitor.visitElement(packageDescriptor, null, annotations);
				if (visitChildren) {
					IJavaElement[] children = fragment.getChildren();
					for (int k = 0; k < children.length; k++) {
						IJavaElement child = children[k];
						if (child instanceof ICompilationUnit) {
							ICompilationUnit unit = (ICompilationUnit) child;
							IType[] allTypes = unit.getAllTypes();
							for (int l = 0; l < allTypes.length; l++) {
								visit(visitor, null, allTypes[l]);
							}
						} else if (child instanceof IClassFile) {
							visit(visitor, null, ((IClassFile)child).getType());
						}
					}
				}
				visitor.endVisitElement(packageDescriptor, null, annotations);
				// visit component overrides
				ManifestNode node = findNode(null, packageDescriptor, false);
				if (node != null) {
					visitOverrides(visitor, node);
				}
			}
		} catch (JavaModelException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * Visits a type.
	 * 
	 * @param visitor
	 * @param owningComponent
	 * @param type
	 */
	private void visit(ApiDescriptionVisitor visitor, String owningComponent, IType type) {
		IElementDescriptor element = getElementDescriptor(type);
		IApiAnnotations annotations = resolveAnnotations(owningComponent, element);
		if (visitor.visitElement(element, owningComponent, annotations)) {
			ManifestNode node = findNode(owningComponent, element, false);
			if (node != null && node.children != null) {
				visitChildren(visitor, node.children);
			}
		}
		visitor.endVisitElement(element, owningComponent, annotations);
		// component specific overrides
		ManifestNode node = findNode(owningComponent, element, false);
		if (node != null ) {
			visitOverrides(visitor, node);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#isInsertOnResolve(java.lang.String, org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	protected boolean isInsertOnResolve(String component, IElementDescriptor elementDescriptor) {
		// only insert for <null> component context, otherwise rules are explicit and should
		// not be inserted
		return component == null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#createNode(org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode, org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	protected ManifestNode createNode(ManifestNode parentNode, IElementDescriptor element) {
		switch (element.getElementType()) {
			case IElementDescriptor.T_PACKAGE:
				try {
					IPackageDescriptor pkg = (IPackageDescriptor) element;
					IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
					for (int i = 0; i < roots.length; i++) {
						IPackageFragmentRoot root = roots[i];
						IClasspathEntry entry = root.getRawClasspathEntry();
						switch (entry.getEntryKind()) {
						case IClasspathEntry.CPE_SOURCE:
						case IClasspathEntry.CPE_LIBRARY:
							IPackageFragment fragment = root.getPackageFragment(pkg.getName());
							if (fragment.exists()) {
								return newPackageNode(fragment, parentNode, element, VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
							}
						}
					}
					return null;
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
					return null;
				}
			case IElementDescriptor.T_REFERENCE_TYPE:
				IReferenceTypeDescriptor descriptor = (IReferenceTypeDescriptor) element;
				try {
					IType type = getJavaProject().findType(descriptor.getQualifiedName().replace('$', '.'));
					if (type != null && type.getJavaProject().equals(getJavaProject())) {
						return newTypeNode(type, parentNode, element, VISIBILITY_INHERITED, RestrictionModifiers.NO_RESTRICTIONS);
					}
				} catch (CoreException e ) {
					ApiPlugin.log(e.getStatus());
					return null;
				}
				return null;
		}
		return super.createNode(parentNode, element);
	}

	/** 
	 * Constructs and returns a new node for the given package fragment.
	 * 
	 * @param fragment
	 * @param parent
	 * @param descriptor
	 * @param vis
	 * @param res
	 * @return
	 */
	PackageNode newPackageNode(IPackageFragment fragment, ManifestNode parent, IElementDescriptor descriptor, int vis, int res) {
		return new PackageNode(fragment, parent, descriptor, vis, res);
	}

	/**
	 * Constructs and returns a new node for the given type.
	 * 
	 * @param type
	 * @param parent
	 * @param descriptor
	 * @param vis
	 * @param res
	 * @return
	 */
	TypeNode newTypeNode(IType type, ManifestNode parent, IElementDescriptor descriptor, int vis, int res) {
		return new TypeNode(type, parent, descriptor, vis, res);
	}
	
	/**
	 * Constructs a new manifiest node.
	 * 
	 * @param parent
	 * @param element
	 * @param vis
	 * @param res
	 * @return
	 */
	ManifestNode newNode(ManifestNode parent, IElementDescriptor element, int vis, int res) {
		return new ManifestNode(parent, element, vis, res);
	}

	/**
	 * Refreshes package nodes if required.
	 */
	private synchronized void refreshPackages() {
		if (fRefreshingInProgress) {
			return;
		}
		// check if in synch
		if (fManifestFile == null || (fManifestFile.getModificationStamp() != fPackageTimeStamp)) {
			try {
				modified();
				fRefreshingInProgress = true;
				// set all existing packages to PRIVATE (could clear
				// the map, but it would be less efficient)
				Iterator iterator = fPackageMap.values().iterator();
				while (iterator.hasNext()) {
					PackageNode node = (PackageNode) iterator.next();
					node.visibility = VisibilityModifiers.PRIVATE;
				}
				fManifestFile = getJavaProject().getProject().getFile(JarFile.MANIFEST_NAME);
				if (fManifestFile.exists()) {
					try {
						IPackageFragment[] fragments = getLocalPackageFragments();
						Set names = new HashSet();
						for (int i = 0; i < fragments.length; i++) {
							names.add(fragments[i].getElementName());
						}
						BundleApiComponent.initializeApiDescription(this, fBundle, names);
						fPackageTimeStamp = fManifestFile.getModificationStamp();
					} catch (CoreException e) {
						ApiPlugin.log(e.getStatus());
					}
				}
			} finally {
				fRefreshingInProgress = false;
			}
		}
	}

	private IElementDescriptor getElementDescriptor(IJavaElement element) {
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				return Factory.packageDescriptor(element.getElementName());
			case IJavaElement.TYPE:
				return Factory.typeDescriptor(((IType)element).getFullyQualifiedName('$'));
			default:
				return null;
		}
	}
	
	/**
	 * Returns the Java project associated with this component.
	 * 
	 * @return associated Java project
	 */
	private IJavaProject getJavaProject() {
		return fProject;
	}

	/**
	 * Returns a class file container for the given package fragment root.
	 * 
	 * @param root package fragment root
	 * @return class file container
	 */
	private synchronized IClassFileContainer getClassFileContainer(IPackageFragmentRoot root) {
		if (fClassFileContainers == null) {
			fClassFileContainers = new HashMap();
		}
		IClassFileContainer container = (IClassFileContainer) fClassFileContainers.get(root);
		if (container == null) {
			container = new SourceFolderClassFileContainer(root, getJavaProject().getElementName());
			fClassFileContainers.put(root, container);
		}
		return container;
	}
	
	/**
	 * Returns all package fragments that originate from this project.
	 * 
	 * @return all package fragments that originate from this project
	 */
	private IPackageFragment[] getLocalPackageFragments() {
		List local = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				// only care about roots originating from this project (binary or source)
				IResource resource = root.getCorrespondingResource();
				if (resource != null && resource.getProject().equals(getJavaProject().getProject())) {
					IJavaElement[] children = root.getChildren();
					for (int j = 0; j < children.length; j++) {
						local.add(children[j]);
					}
				}
			}
		} catch (JavaModelException e) {
			ApiPlugin.log(e.getStatus());
		}
		return (IPackageFragment[]) local.toArray(new IPackageFragment[local.size()]);
	}
	
	/**
	 * Connects this API description to the given bundle.
	 * 
	 * @param bundle bundle description
	 */
	synchronized void connect(BundleDescription bundle) {
		if (fBundle != null && fBundle != bundle) {
			throw new IllegalStateException("Already connected to a bundle"); //$NON-NLS-1$
		}
		fBundle = bundle;
	}
	
	/**
	 * Returns the bundle this description is connected to or <code>null</code>
	 * 
	 * @return connected bundle or <code>null</code>
	 */
	BundleDescription getConnection() {
		return fBundle;
	}
	
	/**
	 * Disconnects this API description from the given bundle.
	 * 
	 * @param bundle bundle description
	 */
	synchronized void disconnect(BundleDescription bundle) {
		if (bundle.equals(fBundle)) {
			fBundle = null;
		} else if (fBundle != null) {
			throw new IllegalStateException("Not connected to same bundle"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns this API description as XML.
	 * 
	 * @throws CoreException
	 */
	synchronized String getXML() throws CoreException {
		Document document = Util.newDocument();	
		Element component = document.createElement(ApiDescriptionProcessor.ELEMENT_COMPONENT);
		component.setAttribute(ApiDescriptionProcessor.ATTR_ID, getJavaProject().getElementName());
		component.setAttribute(ApiDescriptionManager.ATTR_MODIFICATION_STAMP, Long.toString(fPackageTimeStamp));
		document.appendChild(component);
		persistChildren(document, component, fPackageMap);
		return Util.serializeDocument(document);
	}

	/**
	 * Persists the elements in the given map as XML elements, appended
	 * to the given xmlElement.
	 *  
	 * @param document XML document
	 * @param xmlElement node to append children no
	 * @param elementMap elements to persist
	 */
	void persistChildren(Document document, Element xmlElement, Map elementMap) {
		Iterator iterator = elementMap.values().iterator();
		while (iterator.hasNext()) {
			ManifestNode node = (ManifestNode) iterator.next();
			node.persistXML(document, xmlElement, null);
			if (node.overrides != null && !node.overrides.isEmpty()) {
				Iterator entries = node.overrides.entrySet().iterator();
				while (entries.hasNext()) {
					Entry entry = (Entry) entries.next();
					String component = (String) entry.getKey();
					ManifestNode override = (ManifestNode) entry.getValue();
					override.persistXML(document, xmlElement, component);
				}
			}
		}
	}
	
	/**
	 * Cleans this API description so it will be re-populated with fresh data.
	 */
	synchronized void clean() {
		fPackageMap.clear();
		fPackageTimeStamp = -1L;
		modified();
	}
}
