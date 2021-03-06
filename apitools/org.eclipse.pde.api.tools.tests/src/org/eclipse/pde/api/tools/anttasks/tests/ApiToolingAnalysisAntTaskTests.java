/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.anttasks.tests;

import java.util.Properties;

import org.eclipse.core.resources.IFolder;

public class ApiToolingAnalysisAntTaskTests extends AntRunnerTestCase {
	public String getTestResourcesFolder() {
		return "apitooling.analysis/";
	}
	protected IFolder newTest(String resources) throws Exception {
		return super.newTest(getTestResourcesFolder() + resources);
	}

	public void test1() throws Exception {
		IFolder buildFolder = newTest("test1");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists());
		IFolder folder = buildFolder.getFolder("deltatest");
		assertTrue("deltatest folder must exist", folder.exists());
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists());
	}
	public void test2() throws Exception {
		IFolder buildFolder = newTest("test2");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		try {
			runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
			assertFalse("An exception must occur", true);
		} catch (Exception e) {
			checkBuildException(e);
		}
	}
	public void test3() throws Exception {
		IFolder buildFolder = newTest("test3");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("current_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		try {
			runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
			assertFalse("An exception must occur", true);
		} catch (Exception e) {
			checkBuildException(e);
		}
	}
	public void test4() throws Exception {
		IFolder buildFolder = newTest("test4");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("current_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		try {
			runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
			assertFalse("An exception must occur", true);
		} catch (Exception e) {
			checkBuildException(e);
		}
	}
	
	/**
	 * Test for with just Exclude list
	 */
	public void test5() throws Exception {
		IFolder buildFolder = newTest("test5");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists());
		IFolder folder = buildFolder.getFolder("deltatest");
		assertTrue("deltatest folder must exist", folder.exists());
		folder = buildFolder.getFolder("deltatest1");
		assertTrue("deltatest1 folder must exist", folder.exists());
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists());
	}
	
	/**
	 * Test for with just Include list
	 */
	public void test6() throws Exception {
		IFolder buildFolder = newTest("test6");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists());
		IFolder folder = buildFolder.getFolder("deltatest2");
		assertTrue("deltatest2 folder must exist", folder.exists());
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists());
	}
	
	/**
	 * Test for with both Exclude and Include list
	 */
	public void test7() throws Exception {
		IFolder buildFolder = newTest("test7");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists());
		IFolder folder = buildFolder.getFolder("deltatest");
		assertTrue("deltatest folder must exist", folder.exists());
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists());
	}
}
