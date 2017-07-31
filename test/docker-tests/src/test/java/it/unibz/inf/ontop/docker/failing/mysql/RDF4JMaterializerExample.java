package it.unibz.inf.ontop.docker.failing.mysql;

/*
 * #%L
 * ontop-quest-sesame
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.materialization.MaterializationParams;
import it.unibz.inf.ontop.rdf4j.materialization.RDF4JMaterializer;
import it.unibz.inf.ontop.rdf4j.query.MaterializationGraphQuery;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.n3.N3Writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class RDF4JMaterializerExample {

	private static final String inputFile = "/mysql/example/exampleBooks.obda";
	private static final String PROPERTY_FILE = "/mysql/example/exampleBooks.properties";
	private static final String outputFile = "src/test/resources/mysql/example/exampleBooks.n3";

	/**
	 * TODO: try with result streaming
	 */
	private static boolean DO_STREAM_RESULTS = false;
	
	public void generateTriples() throws Exception {

		Class<? extends RDF4JMaterializerExample> klass = getClass();

		OntopSQLOWLAPIConfiguration configuration = OntopSQLOWLAPIConfiguration.defaultBuilder()
				.nativeOntopMappingFile(klass.getResource(inputFile).getPath())
				.propertyFile(klass.getResource(PROPERTY_FILE).getPath())
				.enableTestMode()
				.build();

		MaterializationParams materializationParams = MaterializationParams.defaultBuilder()
				.enableDBResultsStreaming(DO_STREAM_RESULTS)
				.build();

		RDF4JMaterializer materializer = RDF4JMaterializer.defaultMaterializer();
		MaterializationGraphQuery graphQuery = materializer.materialize(configuration, materializationParams);
		
		/*
		 * Print the triples into an external file.
		 */
		File fout = new File(outputFile);
		if (fout.exists()) {
			fout.delete(); // clean any existing output file.
		}
		
		BufferedWriter out = null;
		try {
		    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fout, true)));
		    RDFWriter writer = new N3Writer(out);
		    writer.startRDF();
			graphQuery.evaluate(writer);

			long numberOfTriples = graphQuery.getTripleCountSoFar();
			System.out.println("Generated triples: " + numberOfTriples);
		} finally {
		    if (out != null) {
		    	out.close();
		    }
		}
	}

	public static void main(String[] args) {
		try {
			RDF4JMaterializerExample example = new RDF4JMaterializerExample();
			example.generateTriples();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
