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
package org.eclipse.pde.api.tools.builder.tests.tags;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * Tests the valid @noreference tags on enum methods
 * 
 * @since 3.5
 */
public class ValidEnumMethodTagTests extends ValidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidEnumMethodTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.ValidMethodTagTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum");
	}

	/**
	 * @return test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidEnumMethodTagTests.class);
	}
	
	/**
	 * Tests the supported @noreference tag on enum methods
	 * using an incremental build
	 */
	public void testValidEnumMethodTag1I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the supported @noreference tag on enum methods
	 * using a full build
	 */
	public void testValidEnumMethodTag1F() {
		deployFullBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the supported @noreference tag on outer enum methods
	 * using an incremental build
	 */
	public void testValidEnumMethodTag2I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the supported @noreference tag on outer enum methods
	 * using a full build
	 */
	public void testValidEnumMethodTag2F() {
		deployFullBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the supported @noreference tag on inner enum methods
	 * using an incremental build
	 */
	public void testValidEnumMethodTag3I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the supported @noreference tag on inner enum methods
	 * using a full build
	 */
	public void testValidEnumMethodTag3F() {
		deployFullBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the supported @noreference tag on a variety of inner / outer enum methods
	 * using an incremental build
	 */
	public void testValidEnumMethodTag4I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the supported @noreference tag on a variety of inner / outer enum methods
	 * using a full build
	 */
	public void testValidEnumMethodTag4F() {
		deployFullBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the supported @noreference tag on enum methods in the default package
	 * using an incremental build
	 */
	public void testValidEnumMethodTag5I() {
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests the supported @noreference tag on enum methods in the default package
	 * using a full build
	 */
	public void testValidEnumMethodTag5F() {
		deployFullBuildTest("", "test5", true);
	}
}