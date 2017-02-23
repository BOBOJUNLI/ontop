package it.unibz.inf.ontop.mapping;

import it.unibz.inf.ontop.exception.DuplicateMappingException;
import it.unibz.inf.ontop.exception.InvalidMappingException;

import it.unibz.inf.ontop.exception.MappingIOException;
import it.unibz.inf.ontop.model.OBDAModel;
import org.eclipse.rdf4j.model.Model;

import java.io.File;
import java.io.Reader;

public interface SQLMappingParser {

    OBDAModel parse(File file) throws InvalidMappingException, DuplicateMappingException, MappingIOException;

    OBDAModel parse(Reader reader) throws InvalidMappingException, DuplicateMappingException, MappingIOException;

    OBDAModel parse(Model mappingGraph) throws InvalidMappingException, DuplicateMappingException;
}
