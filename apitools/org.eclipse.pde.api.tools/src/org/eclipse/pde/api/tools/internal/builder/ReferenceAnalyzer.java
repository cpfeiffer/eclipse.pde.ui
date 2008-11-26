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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.builder.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

import com.ibm.icu.text.MessageFormat;

/**
 * The reference analyzer
 * 
 * @since 1.1
 */
public class ReferenceAnalyzer {

	/**
	 * Natural log of 2.
	 */
	private static final double LOG2 = Math.log(2);
	
	/**
	 * Constant used for controlling tracing in the search engine
	 */
	private static boolean DEBUG = Util.DEBUG;	
	
	/**
	 * Empty result collection.
	 */
	private static final IApiProblem[] EMPTY_RESULT = new IApiProblem[0];	
	
	/**
	 * Visits each class file, extracting references.
	 */
	class Visitor extends ApiTypeContainerVisitor {
		
		private IProgressMonitor fMonitor = null;
		
		public Visitor(IProgressMonitor monitor) {
			fMonitor = monitor;
		}

		public void end(IApiComponent component) {
		}

		public boolean visit(IApiComponent component) {
			return true;
		}
		
		public boolean visitPackage(String packageName) {
			fMonitor.subTask(MessageFormat.format(SearchMessages.SearchEngine_0, new String[]{packageName}));
			return true;
		}

		public void endVisitPackage(String packageName) {
			fMonitor.worked(1);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor#visit(java.lang.String, org.eclipse.pde.api.tools.model.component.IClassFile)
		 */
		public void visit(String packageName, IApiTypeRoot classFile) {
			if (!fMonitor.isCanceled()) {
				try {
					IApiType type = classFile.getStructure();
					List references = type.extractReferences(fAllReferenceKinds, null);
					// keep potential matches
					Iterator iterator = references.iterator();
					while (iterator.hasNext()) {
						IReference ref = (IReference) iterator.next();
						// compute index of interested problem detectors
						int index = getLog2(ref.getReferenceKind());
						IApiProblemDetector[] detectors = fIndexedDetectors[index];
						boolean added = false;
						if (detectors != null) {
							for (int i = 0; i < detectors.length; i++) {
								IApiProblemDetector detector = detectors[i];
								if (detector.considerReference(ref)) {
									if (!added) {
										fReferences.add(ref);
										added = true;
									}
								}
							}
						}
					}
				} catch (CoreException e) {
					fStatus.add(e.getStatus());
				}
			}
		}
	}
	
	/**
	 * Scan status
	 */
	private MultiStatus fStatus;	
	
	/**
	 * Bit mask of reference kinds that problem detectors care about.
	 */
	private int fAllReferenceKinds = 0;
	
	/**
	 * List of references to consider/resolve.
	 */
	private List fReferences = new LinkedList();
	
	/**
	 * Problem detectors indexed by the log base 2 of each reference kind they
	 * are interested in. Provides a fast way to hand references off to interested
	 * problem detectors.
	 */
	private IApiProblemDetector[][] fIndexedDetectors;

	/**
	 * Performs the actual reference extraction and analysis.
	 * 
	 * @param scope the scope to extract and analyze references from
	 * @param detectors problem detectors to use
	 * @param monitor progress monitor
	 * @return any problems
	 * @throws CoreException
	 */
	private IApiProblem[] analyze(IApiTypeContainer scope, IApiProblemDetector[] detectors, IProgressMonitor monitor) throws CoreException {
		try {
			// 1. index problem detectors
			indexProblemDetectors(detectors);
			// 2. extract references
			SubMonitor localMonitor = SubMonitor.convert(monitor,SearchMessages.SearchEngine_2, 3);
			localMonitor.subTask(SearchMessages.SearchEngine_3); 
			extractReferences(scope, localMonitor);
			localMonitor.worked(1);
			if (localMonitor.isCanceled()) {
				return EMPTY_RESULT;
			}
			// 3. resolve problematic references
			localMonitor.subTask(SearchMessages.SearchEngine_3);
			resolveReferences(fReferences, localMonitor);
			localMonitor.worked(1);
			if (localMonitor.isCanceled()) {
				return EMPTY_RESULT;
			}		
			// 4. create problems
			List allProblems = new LinkedList();
			localMonitor.subTask(SearchMessages.SearchEngine_3);
			for (int i = 0; i < detectors.length; i++) {
				IApiProblemDetector detector = detectors[i];
				List problems = detector.createProblems();
				allProblems.addAll(problems);
				if (localMonitor.isCanceled()) {
					return EMPTY_RESULT;
				}
			}
			IApiProblem[] array = (IApiProblem[]) allProblems.toArray(new IApiProblem[allProblems.size()]);
			localMonitor.worked(1);
			localMonitor.done();
			return array;
		} finally {
			// clean up
			fIndexedDetectors = null;
			fReferences.clear();
		}
	}
	
	/**
	 * Indexes the problem detectors by the reference kinds they are interested in.
	 * For example, a detector interested in a
	 * {@link org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers#REF_INSTANTIATE}
	 * will be in the 26th index (0x1 << 27, which is 2 ^ 26).
	 * Also initializes the bit mask of all interesting reference kinds.
	 * 
	 * @param detectors problem detectors
	 */
	private void indexProblemDetectors(IApiProblemDetector[] detectors) {
		fIndexedDetectors = new IApiProblemDetector[32][];
		for (int i = 0; i < detectors.length; i++) {
			IApiProblemDetector detector = detectors[i];
			int kinds = detector.getReferenceKinds();
			fAllReferenceKinds |= kinds;
			int mask = 0x1;
			for (int bit = 0; bit < 32; bit++) {
				if ((mask & kinds) > 0) {
					IApiProblemDetector[] indexed = fIndexedDetectors[bit];
					if (indexed == null) {
						fIndexedDetectors[bit] = new IApiProblemDetector[]{detector};
					} else {
						IApiProblemDetector[] next = new IApiProblemDetector[indexed.length + 1];
						System.arraycopy(indexed, 0, next, 0, indexed.length);
						next[indexed.length] = detector;
						fIndexedDetectors[bit] = next;
					}
				}
				mask = mask << 1;
			}
		}
	}
	
	/**
	 * log 2 (x) = ln(x) / ln(2)
	 * 
	 * @param bitConstant a single bit constant (0x1 << n)
	 * @return log base 2 of the constant (the power of 2 the constant is equal to)
	 */
	private int getLog2(int bitConstant) {
		double logX = Math.log(bitConstant);
		double pow = logX / LOG2;
		return (int)Math.round(pow);
	}	
	
	/**
	 * Scans the given scope extracting all reference information.
	 * 
	 * @param scope scope to scan
	 * @param monitor progress monitor
	 * @exception CoreException if the scan fails
	 */
	private void extractReferences(IApiTypeContainer scope, IProgressMonitor monitor) throws CoreException {
		fStatus = new MultiStatus(ApiPlugin.PLUGIN_ID, 0, SearchMessages.SearchEngine_1, null); 
		String[] packageNames = scope.getPackageNames();
		SubMonitor localMonitor = SubMonitor.convert(monitor, packageNames.length);
		ApiTypeContainerVisitor visitor = new Visitor(localMonitor);
		long start = System.currentTimeMillis();
		try {
			scope.accept(visitor);
		} catch (CoreException e) {
			fStatus.add(e.getStatus());
		}
		long end = System.currentTimeMillis();
		if (!fStatus.isOK()) {
			throw new CoreException(fStatus);
		}
		localMonitor.done();
		if (DEBUG) {
			System.out.println("Reference Analyzer: extracted " + fReferences.size() + " references in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}	

	/**
	 * Resolves retained references.
	 * 
	 * @param references list of {@link IReference} to resolve
	 * @param progress monitor
	 * @throws CoreException if something goes wrong
	 */
	private void resolveReferences(List references, IProgressMonitor monitor) throws CoreException {
		// sort references by target type for 'shared' resolution
		Map sigtoref = new HashMap(50);
		
		List refs = null;
		IReference ref = null;
		String key = null;
		List methodDecls = new ArrayList(1000);
		long start = System.currentTimeMillis();
		Iterator iterator = references.iterator();
		while (iterator.hasNext()) {
			ref = (IReference) iterator.next();
			if (ref.getReferenceKind() == ReferenceModifiers.REF_OVERRIDE) {
				methodDecls.add(ref);
			} else {
				key = createSignatureKey(ref);
				refs = (List) sigtoref.get(key);
				if(refs == null) {
					refs = new ArrayList(20);
					sigtoref.put(key, refs);
				}
				refs.add(ref);
			}
		}
		if (monitor.isCanceled()) {
			return;
		}
		long end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Reference Analyzer: split into " + methodDecls.size() + " method overrides and " + sigtoref.size() + " unique references (" + (end - start) + "ms)");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		// resolve references
		start = System.currentTimeMillis();
		resolveReferenceSets(sigtoref, monitor);
		end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Reference Analyzer: resolved unique references in " + (end - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}
		// resolve method overrides
		start = System.currentTimeMillis();
		iterator = methodDecls.iterator();
		while (iterator.hasNext()) {
			Reference reference = (Reference) iterator.next();
			reference.resolve();
		}
		end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Reference Analyzer: resolved method overrides in " + (end - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	/**
	 * Creates a unique string key for a given reference.
	 * The key is of the form "component X references type/member"
	 * <pre>
	 * [component_id]#[type_name](#[member_name]#[member_signature])
	 * </pre>
	 * @param reference reference
	 * @return a string key for the given reference.
	 */
	private String createSignatureKey(IReference reference) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(reference.getMember().getApiComponent().getId());
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(reference.getReferencedTypeName());
		switch (reference.getReferenceType()) {
		case IReference.T_FIELD_REFERENCE:
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(reference.getReferencedMemberName());
			break;
		case IReference.T_METHOD_REFERENCE:
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(reference.getReferencedMemberName());
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(reference.getReferencedSignature());
			break;
		}
		return buffer.toString();
	}	

	/**
	 * Resolves the collect sets of references.
	 * @param map the mapping of keys to sets of {@link IReference}s
	 * @throws CoreException if something bad happens
	 */
	private void resolveReferenceSets(Map map, IProgressMonitor monitor) throws CoreException {
		Iterator types = map.keySet().iterator();
		String key = null;
		List refs = null;
		IReference ref= null;
		while (types.hasNext()) {
			if (monitor.isCanceled()) {
				return;
			}
			key = (String) types.next();
			refs = (List) map.get(key);
			ref = (IReference) refs.get(0);
			((org.eclipse.pde.api.tools.internal.builder.Reference)ref).resolve();
			IApiMember resolved = ref.getResolvedReference();
			if (resolved != null) {
				Iterator iterator = refs.iterator();
				while (iterator.hasNext()) {
					Reference ref2 = (Reference) iterator.next();
					ref2.setResolution(resolved);
				}
			}
		}
	}

	
	/**
	 * Analyzes the given {@link IApiComponent} within the given {@link IApiTypeContainer} (scope) and returns 
	 * a collection of detected {@link IApiProblem}s or an empty collection, never <code>null</code>
	 * @param component
	 * @param scope
	 * @param monitor
	 * @return the collection of detected {@link IApiProblem}s or an empty collection, never <code>null</code>
	 * @throws CoreException
	 */
	public IApiProblem[] analyze(IApiComponent component, IApiTypeContainer scope, IProgressMonitor monitor) throws CoreException {
		// build problem detectors
		IApiProblemDetector[] detectors = buildProblemDetectors(component);
		// analyze
		return analyze(scope, detectors, monitor);
	}
	
	/**
	 * Builds problem detectors to use when analyzing the given component.
	 * 
	 * @param component component to be analyzed
	 * @return problem detectors
	 */
	private IApiProblemDetector[] buildProblemDetectors(IApiComponent component) {
		long start = System.currentTimeMillis();
		IApiComponent[] components = component.getBaseline().getPrerequisiteComponents(new IApiComponent[]{component});
		final ProblemDetectorBuilder visitor = new ProblemDetectorBuilder(component);
		for (int i = 0; i < components.length; i++) {
			IApiComponent prereq = components[i];
			if (!prereq.equals(component)) {
				visitor.setOwningComponent(prereq);
				try {
					prereq.getApiDescription().accept(visitor);
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
				}
			}
		}
		long end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Time to build problem detectors: " + (end-start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}		
		// add names from the leak component as well
		ApiDescriptionVisitor nameVisitor = new ApiDescriptionVisitor() {
			/* (non-Javadoc)
			 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
			 */
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				if (element.getElementType() == IElementDescriptor.PACKAGE) {
					if (VisibilityModifiers.isPrivate(description.getVisibility())) {
						visitor.addNonApiPackageName(((IPackageDescriptor)element).getName());
					}
				}
				return false;
			}
		};
		try {
			component.getApiDescription().accept(nameVisitor);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		List detectors = visitor.getProblemDetectors();
		return (IApiProblemDetector[]) detectors.toArray(new IApiProblemDetector[detectors.size()]);
	}
}