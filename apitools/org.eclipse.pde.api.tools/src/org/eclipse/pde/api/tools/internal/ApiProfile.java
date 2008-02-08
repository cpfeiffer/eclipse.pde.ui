/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.launching.EEVMType;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Implementation of an API profile.
 * 
 * @since 1.0.0
 */
public class ApiProfile implements IApiProfile, Cloneable {
	/**
	 * Constant used for controlling tracing in the example class
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the example class
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}

	public static IWorkspaceRoot ROOT;
	public static IPath ROOT_LOCATION_PATH;

	static {
		try {
			ROOT = ResourcesPlugin.getWorkspace().getRoot();
			ROOT_LOCATION_PATH = ROOT.getLocation();
		} catch(IllegalStateException e) {
			// ignore
		}
	}
	private IApiComponent[] EMPTY_COMPONENTS = new IApiComponent[0];
	
	/**
	 * profile name
	 */
	private String fName;
	
	/**
	 * profile symbolic name
	 */
	private String fId;
	
	/**
	 * profile version identifier
	 */
	private String fVersion;
	
	/**
	 * OSGi bundle state
	 */
	private State fState;
	
	/**
	 * Maps bundle descriptions to components 
	 */
	private Map fComponents = new HashMap();
	
	/**
	 * Maps component id's to components
	 */
	private Map fComponentsById = new HashMap();
	
	/**
	 * Set of enabled components.
	 */
	private Set fEnabledComponents = new HashSet();
	
	/**
	 * Next available bundle id
	 */
	private long fNextId = 0L; 
	
	/**
	 * Execution environment identifier
	 */
	private String fExecutionEnvironment;
	
	/**
	 * Component representing the system library
	 */
	private IApiComponent fSystemLibraryComponent;

	/**
	 * Constant to match any value for ws, os, arch.
	 */
	private AnyValue ANY_VALUE = new AnyValue("*"); //$NON-NLS-1$
	
	/**
	 * Cache of resolved packages. Map of <packageName> -> <Map of <componentName> -> <IApiComponent[]>>.
	 * For each package the cache contains a map of API components that provide that package,
	 * by source component name (including the <code>null</code> component name).
	 */
	private HashMap fComponentsCache = new HashMap();
	
	/**
	 * Cache of system package names
	 */
	private Set fSystemPackageNames = null;

	/**
	 * Constructs a new API profile with the given attributes.
	 * 
	 * @param name profile name
	 * @param id profile identifier
	 * @param version profile version identifier
	 */
	ApiProfile(String name, String id, String version) {
		fName = name;
		fId = id;
		fVersion = version;
		fState = StateObjectFactory.defaultFactory.createState(true);
	}	
	
	/**
	 * Constructor for cloning, not to be used externally
	 */
	private ApiProfile() {}
	
	/**
	 * Constructs a new API profile with the given attributes.
	 * 
	 * @param name profile name
	 * @param id profile identifier
	 * @param version profile version identifier
	 * @param eeDescriptoin execution environment description file
	 * @throws CoreException if unable to create a profile with the given attributes
	 */
	public ApiProfile(String name, String id, String version, File eeDescription) throws CoreException {
		this(name, id, version);
		EEVMType.clearProperties(eeDescription);
		String profile = EEVMType.getProperty(EEVMType.PROP_CLASS_LIB_LEVEL, eeDescription);
		initialize(profile, eeDescription);
	}
	
	/**
	 * Constructs a new API profile with the given attributes.
	 * 
	 * @param name profile name
	 * @param id profile identifier
	 * @param version profile version identifier
	 * @param profile execution environment profile
	 * @param description execution environment description file
	 * @throws CoreException if unable to create a profile with the given attributes
	 */
	public ApiProfile(String name, String id, String version, Properties profile, File description) throws CoreException {
		this(name, id, version);
		initialize(profile, description);
	}	
	
	/**
	 * Constructs a new API profile with the given attributes.
	 * 
	 * @param name profile name
	 * @param id profile identifier
	 * @param version profile version identifier
	 * @param profile execution environment profile file
	 * @param description execution environment description file
	 * @throws CoreException if unable to create a profile with the given attributes
	 */
	public ApiProfile(String name, String id, String version, File profile, File description) throws CoreException {
		this(name, id, version);
		initialize(profile, description);
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		ApiProfile clone = new ApiProfile();
		clone.fId = fId;
		clone.fName = fName;
		clone.fNextId = fNextId;
		clone.fState = fState.getFactory().createState(fState);
		clone.fVersion = fVersion;
		clone.fSystemLibraryComponent = fSystemLibraryComponent;
		clone.fComponents = new HashMap(fComponents);
		clone.fComponentsById = new HashMap(fComponentsById);
		clone.fEnabledComponents = new HashSet(fEnabledComponents);
		clone.fExecutionEnvironment = fExecutionEnvironment;
		return clone;
	}
	
	/**
	 * Initializes this profile to resolve in the execution environment
	 * associated with the given symbolic name.
	 * 
	 * @param environmentId execution environment symbolic name
	 * @param eeFile execution environment description file
	 * @throws CoreException if unable to initialize based on the given id
	 */
	private void initialize(String environmentId, File eeFile) throws CoreException {
		Properties properties = null;
		if (ApiPlugin.isRunningInFramework()) {
			properties = getJavaProfileProperties(environmentId);
		} else {
			properties = Util.getEEProfile(eeFile);
		}
		if (properties == null) {
			abort("Unknown execution environment: " + environmentId, null); //$NON-NLS-1$
		} else {
			initialize(properties, eeFile);
		}
	}
	
	/**
	 * Throws a core exception with the given message and underlying exception,
	 * if any.
	 * 
	 * @param message error message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	private static void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, message, e));
	}	
	
	/**
	 * Returns the property file for the given environment or <code>null</code>.
	 * 
	 * @param ee execution environment symbolic name
	 * @return properties file or <code>null</code> if none
	 */
	public static Properties getJavaProfileProperties(String ee) throws CoreException {
		Bundle osgiBundle = Platform.getBundle("org.eclipse.osgi"); //$NON-NLS-1$
		if (osgiBundle == null) 
			return null;
		URL profileURL = osgiBundle.getEntry(ee.replace('/', '_') + ".profile"); //$NON-NLS-1$
		if (profileURL != null) {
			InputStream is = null;
			try {
				profileURL = FileLocator.resolve(profileURL);
				URLConnection openConnection = profileURL.openConnection();
				openConnection.setUseCaches(false);
				is = openConnection.getInputStream();
				if (is != null) {
					Properties profile = new Properties();
					profile.load(is);
					return profile;
				}
			} catch (IOException e) {
				abort("Unable to read profile: " + ee, e); //$NON-NLS-1$
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return null;
	}		
	
	/**
	 * Initializes this profile to resolve in the execution environment
	 * associated with the given properties file.
	 * 
	 * @param profileFile properties file describing an execution environment profile
	 * @param description execution environment description file
	 * @throws CoreException if unable to initialize based on the given path
	 */
	private void initialize(File profileFile, File description) throws CoreException {
		InputStream is = null;
		Properties profile = null;
		try {
			is = new FileInputStream(profileFile);
			profile = new Properties();
			profile.load(is);
		} catch (IOException e) {
			abort("Unable to read profile: " + profileFile.getAbsolutePath(), e); //$NON-NLS-1$
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				ApiPlugin.log(e);
			}
		}
		initialize(profile, description);
	}	
	
	/**
	 * Initializes this profile from the given properties.
	 * 
	 * @param profile OGSi profile properties
	 * @param description execution environment description file
	 * @throws CoreException if unable to initialize
	 */
	private void initialize(Properties profile, File description) throws CoreException {
		String value = profile.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
		Dictionary dictionary = new Hashtable();
		String[] systemPackages = null;
		if (value != null) {
			systemPackages = value.split(","); //$NON-NLS-1$
			dictionary.put(Constants.FRAMEWORK_SYSTEMPACKAGES, value);
		}
		value = profile.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
		if (value != null) {
			dictionary.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, value);
		}
		fExecutionEnvironment = profile.getProperty("osgi.java.profile.name"); //$NON-NLS-1$
		if (fExecutionEnvironment == null) {
			abort("Profile file missing 'osgi.java.profile.name'" , null); //$NON-NLS-1$
		}
		dictionary.put("osgi.os", ANY_VALUE); //$NON-NLS-1$
		dictionary.put("osgi.arch", ANY_VALUE); //$NON-NLS-1$
		dictionary.put("osgi.ws", ANY_VALUE); //$NON-NLS-1$
		dictionary.put("osgi.nl", ANY_VALUE); //$NON-NLS-1$
		fState.setPlatformProperties(dictionary);
		fSystemLibraryComponent = new SystemLibraryApiComponent(this, description, systemPackages);
		fComponentsById.put(fSystemLibraryComponent.getId(), fSystemLibraryComponent);
		fEnabledComponents.add(fSystemLibraryComponent);
	}

	/* (non-Javadoc)
	 * @see IApiProfile#addApiComponents(org.eclipse.pde.api.tools.model.component.IApiComponent[], boolean)
	 */
	public void addApiComponents(IApiComponent[] components, boolean enabled) {
		for (int i = 0; i < components.length; i++) {
			BundleApiComponent component = (BundleApiComponent) components[i];
			if (component.isSourceComponent()) continue;
			BundleDescription description = component.getBundleDescription();
			fState.addBundle(description);
			this.storeBundleDescription(description, component);
			fComponentsById.put(component.getId(), component);
			if (enabled) {
				fEnabledComponents.add(component);
			}
		}
		fState.resolve();
		if (DEBUG) {
			ResolverError[] errors = getErrors();
			int length = errors.length;
			if (length != 0) {
				System.out.println("Errors found during state resolution"); //$NON-NLS-1$
				for (int i = 0; i < length; i++) {
					ResolverError resolverError = errors[i];
					System.err.println(resolverError);
				}
				System.out.println("All components added to the state"); //$NON-NLS-1$
				BundleDescription[] bundles = fState.getBundles();
				Arrays.sort(bundles, new Comparator() {
					public int compare(Object o1, Object o2) {
						BundleDescription bundleDescription1 = (BundleDescription) o1;
						BundleDescription bundleDescription2 = (BundleDescription) o2;
						return bundleDescription1.getSymbolicName().compareTo(bundleDescription2.getSymbolicName());
					}
				});
				for (int i = 0, max = bundles.length; i < max; i++) {
					BundleDescription bundleDescription = bundles[i];
					System.out.println("bundle descriptions added to the state[" + i + "] : " + bundleDescription.getSymbolicName());
				}
				System.out.println("All available components"); //$NON-NLS-1$
				Arrays.sort(components, new Comparator() {
					public int compare(Object o1, Object o2) {
						IApiComponent component1 = (IApiComponent) o1;
						IApiComponent component2 = (IApiComponent) o2;
						return component1.getId().compareTo(component2.getId());
					}
				});
				for (int i = 0, max = components.length; i < max; i++) {
					IApiComponent component = components[i];
					if (component instanceof PluginProjectApiComponent) {
						System.out.println("workspace component[" + i + "] : " + component);
					} else {
						System.out.println("Binary component   [" + i + "] : " + component);
					}
				}
			} else {
				System.out.println("No errors found during state resolution"); //$NON-NLS-1$
			}
		}
	}

	/* (non-Javadoc)
	 * @see IApiProfile#getApiComponents()
	 */
	public IApiComponent[] getApiComponents() {
		Collection values = fComponentsById.values();
		return (IApiComponent[]) values.toArray(new IApiComponent[values.size()]);
	}

	/* (non-Javadoc)
	 * @see IApiProfile#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see IApiProfile#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see IApiProfile#getVersion()
	 */
	public String getVersion() {
		return fVersion;
	}

	/* (non-Javadoc)
	 * @see IApiProfile#removeApiComponents(org.eclipse.pde.api.tools.model.IApiComponent[])
	 */
	public void removeApiComponents(IApiComponent[] components) {
		for (int i = 0; i < components.length; i++) {
			BundleApiComponent component = (BundleApiComponent) components[i];
			fState.removeBundle(component.getBundleDescription());
			fComponents.remove(component);
			fComponentsById.remove(component.getId());
		}
		fState.resolve();
	}

	/* (non-Javadoc)
	 * @see IApiProfile#resolvePackage(org.eclipse.pde.api.tools.model.IApiComponent, java.lang.String)
	 */
	public synchronized IApiComponent[] resolvePackage(IApiComponent sourceComponent, String packageName) throws CoreException {
		HashMap componentsForPackage = (HashMap) this.fComponentsCache.get(packageName);
		IApiComponent[] cachedComponents = null;
		if (componentsForPackage != null) {
			cachedComponents = (IApiComponent[]) componentsForPackage.get(sourceComponent);
			if (cachedComponents != null) {
				return cachedComponents;
			}
		} else {
			componentsForPackage = new HashMap();
			this.fComponentsCache.put(packageName, componentsForPackage);
		}
		// check system packages first
		if (isSystemPackage(packageName)) {
			cachedComponents = new IApiComponent[] { fSystemLibraryComponent };
		} else {
			if (sourceComponent != null) {
				List componentsList = new ArrayList();
				resolvePackage0(sourceComponent, packageName, componentsList);
				if (componentsList.size() != 0) {
					cachedComponents = new IApiComponent[componentsList.size()];
					componentsList.toArray(cachedComponents);
				}
			}
		}
		if (cachedComponents == null) {
			cachedComponents = EMPTY_COMPONENTS;
		}
		componentsForPackage.put(sourceComponent, cachedComponents);
		return cachedComponents;
	}

	private void resolvePackage0(IApiComponent component, String packageName, List componentsList) throws CoreException {
		BundleDescription bundle = ((BundleApiComponent)component).getBundleDescription();
		if (bundle != null) {
			StateHelper helper = fState.getStateHelper();
			ExportPackageDescription[] visiblePackages = helper.getVisiblePackages(bundle);
			for (int i = 0; i < visiblePackages.length; i++) {
				ExportPackageDescription pkg = visiblePackages[i];
				if (packageName.equals(pkg.getName())) {
					BundleDescription bundleDescription = pkg.getExporter();
					IApiComponent exporter = this.getBundleDescription(bundleDescription);
					if (exporter != null) {
						if (pkg.isRoot()) {
							componentsList.add(exporter);
						} else {
							resolvePackage0(exporter, packageName, componentsList);
						}
					}
				}
			}
			// check for package within the source component
			String[] packageNames = component.getPackageNames();
			// TODO: would be more efficient to have containsPackage(...) or something
			for (int i = 0; i < packageNames.length; i++) {
				if (packageName.equals(packageNames[i])) {
					componentsList.add(component);
				}
			}
		}
	}
	/**
	 * Returns whether the specified package is supplied by the system
	 * library.
	 * 
	 * @param packageName package name
	 * @return whether the specified package is supplied by the system
	 * 	library 
	 */
	private boolean isSystemPackage(String packageName) {
		if (packageName.startsWith("java.")) { //$NON-NLS-1$
			return true;
		}
		if (fSystemPackageNames == null) {
			ExportPackageDescription[] systemPackages = fState.getSystemPackages();
			fSystemPackageNames = new HashSet(systemPackages.length);
			for (int i = 0; i < systemPackages.length; i++) {
				fSystemPackageNames.add(systemPackages[i].getName());
			}
		}
		return fSystemPackageNames.contains(packageName);
	}

	
	/* (non-Javadoc)
	 * @see IApiProfile#newApiComponent(java.lang.String)
	 */
	public IApiComponent newApiComponent(String location) throws CoreException {
		BundleApiComponent component = new BundleApiComponent(this, location);
		if(component.isValidBundle()) {
			component.init(fState, nextId());
			return component;
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see IApiProfile#newApiComponent(IPluginModelBase)
	 */
	public IApiComponent newApiComponent(IPluginModelBase model) throws CoreException {
		BundleDescription bundleDescription = model.getBundleDescription();
		if (bundleDescription == null) {
			return null;
		}
		String location = bundleDescription.getLocation();
		if (location == null) {
			return null;
		}
		IPath pathForLocation = new Path(location);
		BundleApiComponent component = null;
		if (ROOT_LOCATION_PATH != null && ROOT_LOCATION_PATH.isPrefixOf(pathForLocation)) {
			if(isValidProject(location)) {
				component = new PluginProjectApiComponent(this, location, model);
			}
		} else {
			component = new BundleApiComponent(this, location);
		}
		if(component != null && component.isValidBundle()) {
			component.init(fState, nextId());
			return component;
		}
		return null;
	}

	/**
	 * Returns if the specified location is a valid api project or not 
	 * @param location
	 * @return true if the location is valid, false otherwise
	 * @throws CoreException
	 */
	private boolean isValidProject(String location) throws CoreException {
		IPath path = new Path(location);
		IProject project = ApiProfile.ROOT.getProject(path.lastSegment());
		return project != null && project.exists() && project.hasNature(ApiPlugin.NATURE_ID);
	}
	
	/**
	 * Returns the next available bundle identifier.
	 * 
	 * @return next available bundle identifier
	 */
	private long nextId() {
		return ++fNextId;
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiState#getApiComponent(java.lang.String)
	 */
	public IApiComponent getApiComponent(String id) {
		return (IApiComponent) fComponentsById.get(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiState#getExecutionEnvironment()
	 */
	public String getExecutionEnvironment() {
		return fExecutionEnvironment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiState#isEnabled(org.eclipse.pde.api.tools.model.component.IApiComponent)
	 */
	public boolean isEnabled(IApiComponent component) {
		return fEnabledComponents.contains(component);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiState#setEnabled(org.eclipse.pde.api.tools.model.component.IApiComponent[])
	 */
	public void setEnabled(IApiComponent[] components) {
		fEnabledComponents.clear();
		for (int i = 0; i < components.length; i++) {
			IApiComponent component = components[i];
			if (fComponentsById.containsKey(component.getId())) {
				fEnabledComponents.add(component);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiState#disable(org.eclipse.pde.api.tools.model.component.IApiComponent)
	 */
	public void disable(IApiComponent component) {
		fEnabledComponents.remove(component);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiState#enable(org.eclipse.pde.api.tools.model.component.IApiComponent)
	 */
	public void enable(IApiComponent component) {
		if (fComponentsById.containsKey(component.getId())) {
			fEnabledComponents.add(component);
		}
	}

	/**
	 * Returns all errors in the state.
	 * 
	 * @return state errors
	 */
	public ResolverError[] getErrors() {
		List errs = new ArrayList();
		BundleDescription[] bundles = fState.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			ResolverError[] errors = fState.getResolverErrors(bundles[i]);
			for (int j = 0; j < errors.length; j++) {
				errs.add(errors[j]);
			}
		}
		return (ResolverError[]) errs.toArray(new ResolverError[errs.size()]);
	}

	/* (non-Javadoc)
	 * @see IApiProfile#setExecutionEnvironment(java.io.File)
	 */
	public void setExecutionEnvironment(File eefile) throws CoreException {
		EEVMType.clearProperties(eefile);
		Properties profile = Util.getEEProfile(eefile);
		if (profile == null) {
			abort("Could not set up the Execution Environment", null); //$NON-NLS-1$
		}
		initialize(profile, eefile);
	}

	/* (non-Javadoc)
	 * @see IApiProfile#setId(java.lang.String)
	 */
	public void setId(String id) {
		if(id != null) {
			fId = id;
		}
	}

	/* (non-Javadoc)
	 * @see IApiProfile#setName(java.lang.String)
	 */
	public void setName(String name) {
		if(name != null) {
			fName = name;
		}
	}

	/* (non-Javadoc)
	 * @see IApiProfile#setVersion(java.lang.String)
	 */
	public void setVersion(String version) {
		if(version != null) {
			fVersion = version;
		}
	}

	/**
	 * Returns a file to the root of the specified bundle or <code>null</code>
	 * if none. Searches for plug-ins based on the "requiredBundles" system
	 * property.
	 * 
	 * @param bundleName symbolic name
	 * @return bundle root or <code>null</code>
	 */
	static File getBundle(String bundleName) {
		String root = System.getProperty("requiredBundles"); //$NON-NLS-1$
		if (root != null) {
			File bundlesRoot = new File(root);
			if (bundlesRoot.exists() && bundlesRoot.isDirectory()) {
				File[] bundles = bundlesRoot.listFiles();
				if (bundles != null) {
					StringBuffer buffer = new StringBuffer(bundleName);
					buffer.append('_');
					String key = String.valueOf(buffer);
					for (int i = 0; i < bundles.length; i++) {
						File file = bundles[i];
						if (file.getName().startsWith(key)) {
							return file;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Retrieve the properties from the OSGi bundle (jar)
	 * 
	 * @param location the location to look
	 * @param ee the id of the execution environment
	 * @return the ee properties file or <code>null</code> if it could not be created 
	 * @throws CoreException
	 */
	static Properties getEEProfile(File location, String ee) throws CoreException {
		String filename = ee + ".profile"; //$NON-NLS-1$
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			// find the input stream to the profile properties file
			if (location.isDirectory()) {
				File file = new File(location, filename);
				if (file.exists())
					is = new FileInputStream(file);
			} else {
				try {
					zipFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry entry = zipFile.getEntry(filename);
					if (entry != null)
						is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					// nothing to do
				}
			}
			if (is != null) {
				Properties profile = new Properties();
				profile.load(is);
				return profile;
			}
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see IApiProfile#dispose()
	 */
	public void dispose() {
		IApiComponent[] components = getApiComponents();
		for (int i = 0; i < components.length; i++) {
			components[i].dispose();
		}
		fComponents.clear();
		fComponentsById.clear();
		fEnabledComponents.clear();
		fComponentsCache.clear();
		if (fSystemPackageNames != null) fSystemPackageNames.clear();
		fSystemLibraryComponent = null;
		fState = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfile#writeProfileDescription(java.io.OutputStream)
	 */
	public void writeProfileDescription(OutputStream stream) throws CoreException {
		String xml = ApiProfileManager.getProfileXML(this);
		try {
			stream.write(xml.getBytes("UTF-8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			abort("Error writing pofile descrition", e); //$NON-NLS-1$
		} catch (IOException e) {
			abort("Error writing pofile descrition", e); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfile#getDependentComponents(org.eclipse.pde.api.tools.IApiComponent[])
	 */
	public IApiComponent[] getDependentComponents(IApiComponent[] components) {
		ArrayList bundles = getBundleDescriptions(components);
		BundleDescription[] bundleDescriptions = fState.getStateHelper().getDependentBundles((BundleDescription[]) bundles.toArray(new BundleDescription[bundles.size()]));
		return getApiComponents(bundleDescriptions);
	}

	/**
	 * Returns an array of API components corresponding to the given bundle descriptions.
	 * 
	 * @param bundles bundle descriptions
	 * @return corresponding API components
	 */
	private IApiComponent[] getApiComponents(BundleDescription[] bundles) {
		ArrayList dependents = new ArrayList(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription bundle = bundles[i];
			IApiComponent component = getApiComponent(bundle.getSymbolicName());
			if (component != null) {
				dependents.add(component);
			}
		}
		return (IApiComponent[]) dependents.toArray(new IApiComponent[dependents.size()]);
	}

	/**
	 * Returns an array of bundle descriptions corresponding to the given API components.
	 * 
	 * @param components API components
	 * @return corresponding bundle descriptions
	 */
	private ArrayList getBundleDescriptions(IApiComponent[] components) {
		ArrayList bundles = new ArrayList(components.length);
		for (int i = 0; i < components.length; i++) {
			IApiComponent component = components[i];
			if (component instanceof BundleApiComponent) {
				bundles.add(((BundleApiComponent)component).getBundleDescription());
			}
		}
		return bundles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfile#getPrerequisiteComponents(org.eclipse.pde.api.tools.IApiComponent[])
	 */
	public IApiComponent[] getPrerequisiteComponents(IApiComponent[] components) {
		ArrayList bundles = getBundleDescriptions(components);
		BundleDescription[] bundlesDescriptions = fState.getStateHelper().getPrerequisites((BundleDescription[]) bundles.toArray(new BundleDescription[bundles.size()]));
		return getApiComponents(bundlesDescriptions);
	}
	
	private IApiComponent getBundleDescription(BundleDescription bundleDescription) {
		return (IApiComponent) this.fComponents.get(bundleDescription.getSymbolicName() + bundleDescription.getVersion().toString());
	}
	
	private void storeBundleDescription(BundleDescription bundleDescription, IApiComponent component) {
		this.fComponents.put(bundleDescription.getSymbolicName() + bundleDescription.getVersion().toString(), component);
	}
	
	/**
	 * Reset the given bundle.
	 * 
	 * @param component
	 * @param description
	 * @throws CoreException 
	 */
	protected synchronized void reset(BundleApiComponent component, BundleDescription description) throws CoreException {
		fComponentsCache.clear();
		if (description != null) {
			fState.removeBundle(description);
		}
		component.init(fState, nextId());
		fState.addBundle(component.getBundleDescription());
		fState.resolve();
	}
	
	/**
	 * Clear cached settings for the given package.
	 * 
	 * @param packageName
	 */
	protected synchronized void clearPackage(String packageName) {
		fComponents.remove(packageName);
	}
}
