/*
  (c) Copyright 2002, Hewlett-Packard Company, all rights reserved.
  [See end of file]
  $Id: TestMultiModel.java,v 1.1 2003-06-18 20:57:40 wkw Exp $
*/

package com.hp.hpl.jena.db.test;

/**
 * 
 * This tests basic operations on the modelRDB.
 * 
 * It adds/removes statements of different types and verifys
 * that the correct statements exist at the correct times.
 * 
 * To run, you must have a mySQL database operational on
 * localhost with a database name of "test" and allow use
 * by a user named "test" with an empty password.
 * 
 * (Based in part on earlier Jena tests by bwm, kers, et al.)
 * 
 * @author csayers
*/

import com.hp.hpl.jena.db.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.DB;

import junit.framework.TestCase;
import junit.framework.TestSuite;



public class TestMultiModel extends TestCase
    {    
        
    public TestMultiModel( String name )
        { super( name ); }
    
    public static TestSuite suite()
        { return new TestSuite( TestMultiModel.class ); }   
     
    Model model = null;
    ModelRDB dmod1 = null;    
	ModelRDB dmod2 = null;
	ModelRDB nmod1 = null; 
	ModelRDB nmod2 = null; 
	IDBConnection conn = null;
	Model dbProperties = null;    
	
    protected void setUp() throws java.lang.Exception {
    	conn = TestConnection.makeAndCleanTestConnection();
		Model props = ModelRDB.getDefaultModelProperties(conn);
		dmod1 = ModelRDB.createModel(conn);
		addToDBGraphProp(props,DB.graphDBSchema,"DEFAULT");
		dmod2 = ModelRDB.createModel(conn, "Def_Model_2",props);
		nmod1 = ModelRDB.createModel(conn,"Named_Model_1");
		props = ModelRDB.getDefaultModelProperties(conn);
		addToDBGraphProp(props,DB.graphDBSchema,"Named_Model_1");
		nmod2 = ModelRDB.createModel(conn,"Named_Model_2",props);
		model = ModelFactory.createDefaultModel();
		dbProperties = conn.getDatabaseProperties();	
    }
    
    protected void tearDown() throws java.lang.Exception {
    	dmod1.close(); dmod2.close();
    	nmod1.close(); nmod2.close();
    	conn.cleanDB();
    	conn.close();
    	conn = null;
    }
    
	
	public void addToDBGraphProp ( Model model, Property prop, String val ) {
		// first, get URI of the graph
		StmtIterator iter = model.listStatements(
			new SimpleSelector(null, DB.graphName, (RDFNode) null));
		assertTrue(iter.hasNext());
		
		Statement stmt = iter.nextStatement();
		assertTrue(iter.hasNext() == false);
		Resource graphURI = stmt.getSubject();
		Literal l = model.createLiteral(val);
		Statement s = model.createStatement(graphURI,prop,l);
		model.add(s);
		assertTrue(model.contains(s));
	}
	
    private void addOnModel(Model model, Statement stmt) {
    	model.add(stmt);
    	assertTrue( model.contains(stmt) );
    	model.add(stmt);
    	assertTrue( model.contains(stmt) );
    }
    
	private void rmvOnModel(Model model, Statement stmt) {
		assertTrue( model.contains(stmt) );
		model.remove(stmt);
		assertTrue( !model.contains(stmt) );
		model.add(stmt);
		assertTrue( model.contains(stmt) );
		model.remove(stmt);
		assertTrue( !model.contains(stmt) );
	}

    
	private void addRemove(Statement stmt) {
		addOnModel(model,stmt);
		addOnModel(dmod1,stmt);
		addOnModel(dmod2,stmt);
		addOnModel(nmod1,stmt);
		addOnModel(nmod2,stmt);
		
		rmvOnModel(nmod2,stmt);
		rmvOnModel(nmod1,stmt);
		rmvOnModel(dmod2,stmt);
		rmvOnModel(dmod1,stmt);
		rmvOnModel(model,stmt);
	}
    
    public void testAddRemoveURI() {
    	Resource s = model.createResource("test#subject");
    	Property p = model.createProperty("test#predicate");
    	Resource o = model.createResource("test#object");
    	
		addRemove( model.createStatement(s,p,o));    		
    }
    
    public void testAddRemoveLiteral() {
    	Resource s = model.createResource("test#subject");
    	Property p = model.createProperty("test#predicate");
    	Literal l = model.createLiteral("testLiteral");
    	
    	addRemove( model.createStatement(s,p,l));    	
    } 

	public long getMaxLiteral() {
		Property p = DB.maxLiteral;
		StmtIterator iter = dbProperties.listStatements(
			new SimpleSelector(null, p, (RDFNode) null));
		assertTrue(iter.hasNext());
		
		Statement stmt = iter.nextStatement();
		assertTrue(iter.hasNext() == false);
		String maxLitStr = stmt.getString();
		/* String maxLitStr = "250"; */
		assertTrue(maxLitStr != null);		
		long maxLit = Integer.parseInt(maxLitStr);

		return maxLit;
	}
	
	public void testGetMaxLiteral() {
		long maxLit = getMaxLiteral();
		assertTrue(maxLit > 0 && maxLit < 100000);
		/* note: 100000 is a soft limit; some DBMS may allow larger max literals */
	}
	
	public void testMaxLiteral() {
		long maxLit = getMaxLiteral();
		assertTrue(maxLit > 0 && maxLit < 100000);

		String base = ".";
		StringBuffer buffer = new StringBuffer(1024+(int)maxLit);
		/* long minMaxLit = maxLit < 1024 ? maxLit - (maxLit/2) : maxLit - 512; */
		/* long maxMaxLit = maxLit + 1024; */
		/* TODO: find out why this test takes sooooo long (minutes!) with the above bounds */
		long minMaxLit = maxLit - 32;
		long maxMaxLit = maxLit + 32;
		assertTrue (minMaxLit > 0);


		Resource s = model.createResource("test#subject");
		Property p = model.createProperty("test#predicate");
		Literal l;
		Statement stmt;
		while(buffer.length() < minMaxLit) { /*( build base string */
			buffer.append(base);	
		}
		/* add stmts with long literals */
		long modelSizeBeg = model.size();
		while(buffer.length() < maxMaxLit) {
			l = model.createLiteral(buffer.toString());
			stmt = model.createStatement(s,p,l);
			model.add(stmt);
			assertTrue(model.contains(stmt));
			assertTrue(stmt.getObject().equals(l));
			buffer.append(base);
		}
		assertTrue ( model.size() == (modelSizeBeg + maxMaxLit - minMaxLit) ); 
		/* remove stmts with long literals */
		while(buffer.length() > minMaxLit ) {
			buffer.deleteCharAt(0);
			l = model.createLiteral(buffer.toString());
			stmt = model.createStatement(s,p,l);
			assertTrue( model.contains(stmt) );
			model.remove(stmt);
			assertTrue( !model.contains(stmt));    	
		}
		assertTrue ( model.size() == modelSizeBeg ); 
	} 
	
	public void testAddRemoveHugeLiteral() {
    	String base = "This is a huge string that repeats.";
    	StringBuffer buffer = new StringBuffer(4096);
    	while(buffer.length() < 4000 )
    		buffer.append(base);
    	Resource s = model.createResource("test#subject");
    	Property p = model.createProperty("test#predicate");
    	Literal l = model.createLiteral(buffer.toString());
    	
    	addRemove( model.createStatement(s,p,l));    	
    } 
    
    public void testAddRemoveDatatype() {
    	Resource s = model.createResource("test#subject");
    	Property p = model.createProperty("test#predicate");
    	Literal l = model.createTypedLiteral("stringType");
    	
    	addRemove( model.createStatement(s,p,l));    	
    } 

    public void testAddRemoveHugeDatatype() {
    	String base = "This is a huge string that repeats.";
    	StringBuffer buffer = new StringBuffer(4096);
    	while(buffer.length() < 4000 )
    		buffer.append(base);
    	Resource s = model.createResource("test#subject");
    	Property p = model.createProperty("test#predicate");
    	Literal l2 = model.createTypedLiteral(buffer.toString());
    	
    	addRemove( model.createStatement(s,p,l2));    	
    } 
    
   public void testAddRemoveBNode() {
    	Resource s = model.createResource();
    	Property p = model.createProperty("test#predicate");
    	Resource o = model.createResource();
    	    	
     	addRemove( model.createStatement(s,p,o));
   	   	
   }
   
   public void testBNodeIdentityPreservation() {
    	Resource s = model.createResource();
    	Property p = model.createProperty("test#predicate");
    	Resource o = model.createResource();
    	 
    	// Create two statements that differ only in
    	// the identity of the bnodes - then perform
    	// add-remove on one and verify the other is
    	// unchanged.
    	Statement spo = model.createStatement(s,p,o);
    	Statement ops = model.createStatement(o,p,s);
    	model.add(spo);
     	addRemove(ops);   	
    	assertTrue( model.contains(spo));
    	model.remove(spo);
   }
   

}
    	

/*
    (c) Copyright Hewlett-Packard Company 2002
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
