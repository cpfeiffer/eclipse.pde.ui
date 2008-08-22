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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder correctly reports compatibility problems
 * related to members in interfaces.
 * 
 * @since 3.4
 */
public class InterfaceCompatibilityMemberTests extends InterfaceCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("org.eclipse.api.tools.tests.compatability.a/src/a/interfaces/members");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.interfaces.members.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public InterfaceCompatibilityMemberTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("members");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InterfaceCompatibilityMemberTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests adding a field to an interface
	 */
	private void xAddField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddField.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.FIELD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddField", "ADDED_FIELD"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddFieldI() throws Exception {
		xAddField(true);
	}	
	
	public void testAddFieldF() throws Exception {
		xAddField(false);
	}
	
	/**
	 * Tests adding a field to a noimplement interface
	 */
	private void xAddFieldNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFieldNoImplement.java");
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddFieldNoImplementI() throws Exception {
		xAddFieldNoImplement(true);
	}	
	
	public void testAddFieldNoImplementF() throws Exception {
		xAddFieldNoImplement(false);
	}

	/**
	 * Tests adding a method to an interface
	 */
	private void xAddMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethod.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddMethod", "addMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddMethodI() throws Exception {
		xAddMethod(true);
	}	
	
	public void testAddMethodF() throws Exception {
		xAddMethod(false);
	}	
	
	/**
	 * Tests adding a method to a noimplement interface
	 */
	private void xAddMethodNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethodNoImplement.java");
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddMethodNoImplementI() throws Exception {
		xAddMethodNoImplement(true);
	}	
	
	public void testAddMethodNoImplementF() throws Exception {
		xAddMethodNoImplement(false);
	}	
	
	/**
	 * Tests adding a  member type to an interface
	 */
	private void xAddMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMemberType.java");
		// TODO: expecting a problem
//		int[] ids = new int[] {
//				ApiProblemFactory.createProblemId(
//						IApiProblem.CATEGORY_COMPATIBILITY,
//						IDelta.INTERFACE_ELEMENT_TYPE,
//						IDelta.ADDED,
//						IDelta.MEMBER_ELEMENT_TYPE)
//		};
//		setExpectedProblemIds(ids);
//		String[][] args = new String[1][];
//		args[0] = new String[]{PACKAGE_PREFIX + "AddMemberType", "MemberType"};
//		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddMemberTypeI() throws Exception {
		xAddMemberType(true);
	}	
	
	public void testAddMemberTypeF() throws Exception {
		xAddMemberType(false);
	}	
		
	/**
	 * Tests adding a  member type to a noimplement interface
	 */
	private void xAddMemberTypeNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMemberTypeNoImplement.java");
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddMemberTypeNoImplementI() throws Exception {
		xAddMemberTypeNoImplement(true);
	}	
	
	public void testAddMemberTypeNoImplementF() throws Exception {
		xAddMemberTypeNoImplement(false);
	}	
	
	/**
	 * Tests removing a field from an interface
	 */
	private void xRemoveField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveField.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.FIELD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveField", "FIELD"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testRemoveFieldI() throws Exception {
		xRemoveField(true);
	}	
	
	public void testRemoveFieldF() throws Exception {
		xRemoveField(false);
	}	
	
	/**
	 * Tests removing a method from an interface
	 */
	private void xRemoveMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveMethod.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveMethod", "removeMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testRemoveMethodI() throws Exception {
		xRemoveMethod(true);
	}	
	
	public void testRemoveMethodF() throws Exception {
		xRemoveMethod(false);
	}
	
	/**
	 * Tests removing a member type from an interface
	 */
	private void xRemoveMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveMemberType.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.TYPE_MEMBER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"MemberType"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testRemoveMemberTypeI() throws Exception {
		xRemoveMemberType(true);
	}	
	
	public void testRemoveMemberTypeF() throws Exception {
		xRemoveMemberType(false);
	}	
}