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
package a.b.c;

public enum test8 {

	A;
	
	/**
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
	
	public enum inner {
		A;
		
		/**
		 * @noreference This enum method is not intended to be referenced by clients.
		 * @noreference This enum method is not intended to be referenced by clients.
		 * @noreference This enum method is not intended to be referenced by clients.
		 */
		public void m1() {
			
		}
	}
}

enum outer {
	B;
	
	/**
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
}
