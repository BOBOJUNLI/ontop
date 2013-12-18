/*
 * Copyright (C) 2009-2013, Free University of Bozen Bolzano
 * This source code is available under the terms of the Affero General Public
 * License v3.
 * 
 * Please see LICENSE.txt for full license terms, including the availability of
 * proprietary exceptions.
 */

package it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht;

import it.unibz.krdb.obda.ontology.Description;
import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.ontology.OntologyFactory;
import it.unibz.krdb.obda.ontology.Property;
import it.unibz.krdb.obda.ontology.PropertySomeRestriction;
import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Starting from a graph build a DAG. 
 * Considering  equivalences and redundancies, it eliminates cycles executes
 * transitive reduction.
	// contains the representative node with the set of equivalent mappings.
 */

/***
 * A map to keep the relationship between 'eliminated' and 'remaining'
 * nodes, created while computing equivalences by eliminating cycles in the
 * graph.
 */

public class DAGBuilder {

	/**
	 * *Construct a DAG starting from a given ontology 
	 * 
	 * @param onto an ontology 
	 */
	
	public static DAGImpl getDAG(Ontology onto) {
		
		TBoxGraph graph = TBoxGraph.getGraph(onto);
		
		DAGImpl dag = DAGBuilder.getDAG(graph);
		
		return dag;
	}


	/**
	 * *Construct a DAG starting from a given graph 
	 * 
	 * @param graph needs a graph with or without cycles 
	 */
	
	public static DAGImpl getDAG(TBoxGraph graph) {

		Map<Description, EquivalenceClass<Description>> equivalencesMap = new HashMap<Description, EquivalenceClass<Description>>();
		Map<Description, Description> replacements = new HashMap<Description, Description>();

		// temporary graph to be transformed in DAG
		DefaultDirectedGraph<Description,DefaultEdge> modifiedGraph = graph.getCopy();

		eliminateCycles(modifiedGraph, equivalencesMap, replacements);
		eliminateRedundantEdges(modifiedGraph);

		DAGImpl dag = new DAGImpl(modifiedGraph, equivalencesMap, replacements);
		return dag;
	}

	/**
	 * *Construct a DAG starting from a given graph with already known
	 * equivalent nodes and representative nodes
	 * 
	 * @param graph needs a graph with or without cycles
	 * @param equivalents a map between the node and its equivalent nodes
	 * @param representatives a map between the node and its representative node
	 */
	public static DAGImpl getDAG(DefaultDirectedGraph<Description,DefaultEdge> graph,
			Map<Description, EquivalenceClass<Description>> equivalencesMap,
			Map<Description, Description> replacements) {

		eliminateCycles(graph, equivalencesMap, replacements);
		eliminateRedundantEdges(graph);

		DAGImpl dag = new DAGImpl(graph, equivalencesMap, replacements);
		return dag;
	}

	/***
	 * Eliminates redundant edges to ensure that the remaining DAG is the
	 * transitive reduction of the original DAG.
	 * 
	 * <p>
	 * This is done with an ad-hoc algorithm that functions as follows:
	 * 
	 * <p>
	 * Compute the set of all nodes with more than 2 outgoing edges (these have
	 * candidate redundant edges.) <br>
	 */
	private static void eliminateRedundantEdges(DefaultDirectedGraph<Description,DefaultEdge> graph) {
		/* Compute the candidate nodes */
		List<Description> candidates = new LinkedList<Description>();
		Set<Description> vertexes = graph.vertexSet();
		for (Description vertex : vertexes) {
			int outdegree = graph.outDegreeOf(vertex);
			if (outdegree > 1) {
				candidates.add(vertex);
			}
		}

		/*
		 * for each candidate x and each outgoing edge x -> y, we will check if
		 * y appears in the set of redundant edges
		 */

		for (Description candidate : candidates) {

			Set<DefaultEdge> possiblyRedundantEdges = new LinkedHashSet<DefaultEdge>();

			possiblyRedundantEdges.addAll(graph.outgoingEdgesOf(candidate));

			Set<DefaultEdge> eliminatedEdges = new HashSet<DefaultEdge>();

			// registering the target of the possible redundant targets for this
			// node
			Set<Description> targets = new HashSet<Description>();

			Map<Description, DefaultEdge> targetEdgeMap = new HashMap<Description, DefaultEdge>();

			for (DefaultEdge edge : possiblyRedundantEdges) {
				Description target = graph.getEdgeTarget(edge);
				targets.add(target);
				targetEdgeMap.put(target, edge);
			}

			for (DefaultEdge currentPathEdge : possiblyRedundantEdges) {
				Description currentTarget = graph.getEdgeTarget(currentPathEdge);
				if (eliminatedEdges.contains(currentPathEdge))
					continue;
				eliminateRedundantEdge(graph, currentPathEdge, targets, targetEdgeMap,
						currentTarget, eliminatedEdges);
			}

		}

	}

	private static void eliminateRedundantEdge(DefaultDirectedGraph<Description,DefaultEdge> graph, 
			DefaultEdge safeEdge,
			Set<Description> targets,
			Map<Description, DefaultEdge> targetEdgeMap,
			Description currentTarget, Set<DefaultEdge> eliminatedEdges) {
		
		if (targets.contains(currentTarget)) {
			DefaultEdge edge = targetEdgeMap.get(currentTarget);
			if (!edge.equals(safeEdge)) {
				/*
				 * This is a redundant edge, removing it.
				 */
				graph.removeEdge(edge);
				eliminatedEdges.add(edge);
			}
		}

		// continue traversing the dag up
		Set<DefaultEdge> edgesInPath = graph.outgoingEdgesOf(currentTarget);
		for (DefaultEdge outEdge : edgesInPath) {
			Description target = graph.getEdgeTarget(outEdge);
			// System.out.println("target "+target+" "+outEdge);
			eliminateRedundantEdge(graph, safeEdge, targets, targetEdgeMap, target, eliminatedEdges);
		}

	}

	/***
	 * Eliminates all cycles in the graph by computing all strongly connected
	 * components and eliminating all but one node in each of the components
	 * from the graph. The result of this transformation is that the graph
	 * becomes a DAG.
	 * 
	 * 
	 * <p>
	 * In the process two objects are generated, an 'Equivalence map' and a
	 * 'replacementMap'. The first can be used to get the implied equivalences
	 * of the TBox. The second can be used to locate the node that is
	 * representative of an eliminated node.
	 * 
	 * <p>
	 * Computation of the strongly connected components is done using Gabow SCC
	 * algorithm.
	 * 
	 */

	private static void eliminateCycles(DefaultDirectedGraph<Description,DefaultEdge> graph, 
			Map<Description, EquivalenceClass<Description>> equivalencesMap, 
			Map<Description, Description> replacements) {

		GabowSCC<Description, DefaultEdge> inspector = new GabowSCC<Description, DefaultEdge>(graph);

		// each set contains vertices which together form a strongly connected
		// component within the given graph
		List<EquivalenceClass<Description>> equivalenceSets = inspector
				.stronglyConnectedSets();

		for (EquivalenceClass<Description> equivalenceSet : equivalenceSets) {
			if (equivalenceSet.size() > 1)
				for (Description node : equivalenceSet) 
					equivalencesMap.put(node, equivalenceSet);
		}
		
		
		OntologyFactory fac = OntologyFactoryImpl.getInstance();

		/*
		 * A set with all the nodes that have been processed as participating in
		 * an equivalence cycle. If a component contains any of these nodes, the
		 * component should be ignored, since a cycle involving the same nodes
		 * or nodes for inverse descriptions has already been processed.
		 */
		Set<Description> processedNodes = new HashSet<Description>();
		Set<Description> chosenNodes = new HashSet<Description>();

		for (EquivalenceClass<Description> equivalenceSet : equivalenceSets) {

			if (equivalenceSet.size() < 2)
				continue;

			/*
			 * Avoiding processing nodes two times, but assign the equivalentMap
			 */
			boolean ignore = false;

			for (Description node : equivalenceSet) {

				if (!ignore && processedNodes.contains(node)) {
					ignore = true;
				}
				// assign to each node the equivalent map set
				//equivalencesMap.put(node, equivalenceSet);

				if (!ignore && !(node instanceof Property)) {
					ignore = true;
				}

			}

			/*
			 * We consider first the elements that are connected with other
			 * named roles, checking if between their parents there is an
			 * already assigned named role or an inverse
			 */

			if (!ignore) {

				Iterator<Description> iterator = equivalenceSet.iterator();
				while (iterator.hasNext()) {
					Description representative = iterator.next();

					Set<DefaultEdge> edges = graph.outgoingEdgesOf(representative);
					for (DefaultEdge outEdge : edges) {
						Description target = graph.getEdgeTarget(outEdge);

						if (processedNodes.contains(target))
							if (((Property) target).isInverse()) {
								ignore = true;
								chosenNodes.addAll(equivalenceSet.getMembers());
								break;
							}

						if (chosenNodes.contains(target)) {
							ignore = true;
							chosenNodes.addAll(equivalenceSet.getMembers());
							break;
						}
					}
					if (ignore)
						break;
				}
			}

			if (ignore)
				continue;

			/* Assign the representative role with its inverse, domain and range
			 * 
			 */
			
			Iterator<Description> iterator = equivalenceSet.iterator();
			Description representative = iterator.next();
			Description notRepresentative = null;

			// if it is inverse I search a not inverse element as representative
			if (((Property) representative).isInverse()) {
				boolean notInverse = false;
				for (Description equivalent : equivalenceSet) {
					if (equivalent instanceof Property
							&& !((Property) equivalent).isInverse()) {
						notRepresentative = representative;
						representative = equivalent;
						notInverse = true;
						break;
					}
				}
				if (!notInverse)
					representative = null; // if all inverse they will be added
											// later in another set with
											// property
			}

			if (representative == null)
				continue;

			Property prop = (Property) representative;
			Description inverse = fac.createProperty(prop.getPredicate(), !prop.isInverse());
			Description domain = fac.createPropertySomeRestriction(prop.getPredicate(), prop.isInverse());
			Description range = fac.createPropertySomeRestriction(prop.getPredicate(), !prop.isInverse());

			processedNodes.add(inverse);
			processedNodes.add(domain);
			processedNodes.add(range);

			processedNodes.add(representative);

			//remove all the equivalent node (eliminatedNode)
			
			while (iterator.hasNext()) {
				Description eliminatedNode = iterator.next();
				if (eliminatedNode.equals(representative)
						& notRepresentative != null)
					eliminatedNode = notRepresentative;
				replacements.put(eliminatedNode, representative);

				removeNodeAndRedirectEdges(graph, eliminatedNode, representative);
				processedNodes.add(eliminatedNode);

				if (eliminatedNode instanceof Property) {

					/*
					 * we are dealing with properties, so we need to also
					 * collapse the inverses and existentials
					 */

					Property equiprop = (Property) eliminatedNode;
					Description equivinverseNode = fac.createProperty(
							equiprop.getPredicate(), !equiprop.isInverse());

					Description equivDomainNode = fac.createPropertySomeRestriction(
									equiprop.getPredicate(), equiprop.isInverse());

					Description equivRangeNode = fac.createPropertySomeRestriction(
									equiprop.getPredicate(), !equiprop.isInverse());

					/* 
					 * Particular case in which the representative is in a cycle
					 * with its inverse. We add everything to the named role. 
					 */
					
					if (equivinverseNode.equals((Property) representative)) {
						removeNodeAndRedirectEdges(graph, equivDomainNode, domain);
						processedNodes.add(equivDomainNode);

						replacements.put(equivDomainNode, domain);
					}

					else {
						removeNodeAndRedirectEdges(graph, equivinverseNode, inverse);
						processedNodes.add(equivinverseNode);
						removeNodeAndRedirectEdges(graph, equivDomainNode, domain);
						processedNodes.add(equivDomainNode);
						removeNodeAndRedirectEdges(graph, equivRangeNode, range);
						processedNodes.add(equivRangeNode);

						replacements.put(equivinverseNode, inverse);
						replacements.put(equivDomainNode, domain);
						replacements.put(equivRangeNode, range);
					}
				}

			}

			
		}

		/*
		 * Second check of the strongly connected components for the classes
		 * 
		 */
		List<EquivalenceClass<Description>> equivalenceClassSets = inspector
				.stronglyConnectedSets();
		Set<Description> processedClassNodes = new HashSet<Description>();

		// processedNodes.clear();

		for (EquivalenceClass<Description> equivalenceClassSet : equivalenceClassSets) {

			if (equivalenceClassSet.size() < 2)
				continue;
			
			boolean ignore = false;
			for (Description node : equivalenceClassSet) {

				if (!ignore && processedClassNodes.contains(node)
						|| node instanceof Property) {
					ignore = true;
					break;

				}

			}
			if (ignore)
				continue;

			Iterator<Description> iterator = equivalenceClassSet.iterator();
			Description node = iterator.next();
			Description representative = replacements.get(node);
			if (representative == null)
				representative = node;

			/*
			 *  if a node has already been assigned I check if it is named,
			 *  if not it is substitute with an equivalent named node if it is present
			 */
			
			if (processedNodes.contains(representative)) {
				Description notRepresentative = null;

				// I want to consider a named class as representative element
				if (representative instanceof PropertySomeRestriction) {
					boolean namedElement = false;
					for (Description equivalent : equivalenceClassSet) {

						if (equivalent instanceof OClass) {
							notRepresentative = representative;
							representative = equivalent;
							namedElement = true;

							break;
						}
					}

					// //if all propertysomerestriction they have been added
					// before when working over roles
					
					if (!namedElement)
						continue;

					for (Description element : equivalenceClassSet) {
						if (element.equals(representative)) {
							replacements.remove(element);
							continue;

						}

						replacements.put(element, representative);
					}

				}
				processedClassNodes.add(representative);

				while (iterator.hasNext()) {

					Description eliminatedNode = iterator.next();
					// I want to consider only the nodes that are not yet been
					// processed
					if (processedNodes.contains(eliminatedNode)
							|| eliminatedNode.equals(notRepresentative))
						continue;

					if (eliminatedNode.equals(representative))
						eliminatedNode = notRepresentative;

					removeNodeAndRedirectEdges(graph, eliminatedNode, representative);

					processedClassNodes.add(eliminatedNode);
				}

			} else { // If a node has not yet been assigned

				
				Description notRepresentative = null;

				// I want to consider a named class as representative element
				if (representative instanceof PropertySomeRestriction) {
					
					for (Description equivalent : equivalenceClassSet) {
						// Description equivalent=replacements.get(e);
						// if(equivalent==null)
						// equivalent=e;
						if (equivalent instanceof OClass) {
							notRepresentative = representative;
							representative = equivalent;
							// namedElement=true;
							// replacements.put(notRepresentative,
							// representative);
							// equivalencesMap.put(notRepresentative,
							// equivalenceSet);
							break;
						}
					}
					
				}

				processedClassNodes.add(representative);
				// equivalencesMap.put(representative, equivalenceClassSet);

				while (iterator.hasNext()) {

					Description eliminatedNode = iterator.next();

					
					if (eliminatedNode.equals(representative)
							& notRepresentative != null)
						eliminatedNode = notRepresentative;

					Description replacement = replacements.get(eliminatedNode);

					replacements.put(eliminatedNode, representative);

					/*
					 * check if the node has been already replaced by an other
					 * in the first part with property if substitute check if
					 * its replacements still has to be processed
					 */
					if (replacement != null)
						if (!processedClassNodes.contains(replacement)) {
							processedClassNodes.add(eliminatedNode);
							eliminatedNode = replacement;
							replacements.put(eliminatedNode, representative);
						} else {
							processedClassNodes.add(eliminatedNode);
							continue;
						}
					
					removeNodeAndRedirectEdges(graph, eliminatedNode, representative);

					processedClassNodes.add(eliminatedNode);
				}
			}
		}

	}
	
	private static void removeNodeAndRedirectEdges(DefaultDirectedGraph<Description,DefaultEdge> graph, 
			Description eliminatedNode, Description representative) {
		/*
		 * Re-pointing all links to and from the eliminated node to
		 * the representative node
		 */

		Set<DefaultEdge> edges = new HashSet<DefaultEdge>(
				graph.incomingEdgesOf(eliminatedNode));

		for (DefaultEdge incEdge : edges) {
			Description source = graph.getEdgeSource(incEdge);
			graph.removeAllEdges(source, eliminatedNode);

			if (source.equals(representative))
				continue;

			graph.addEdge(source, representative);
		}

		edges = new HashSet<DefaultEdge>(
				graph.outgoingEdgesOf(eliminatedNode));

		for (DefaultEdge outEdge : edges) {
			Description target = graph.getEdgeTarget(outEdge);
			graph.removeAllEdges(eliminatedNode, target);

			if (target.equals(representative))
				continue;
			
			graph.addEdge(representative, target);
		}

		graph.removeVertex(eliminatedNode);		
	}
	

	/*
	 * Alterntative version but slower to eliminate redundances public void
	 * eliminateRedundances(){ LinkedList<DefaultEdge> redundantEdges = new
	 * LinkedList<DefaultEdge>(); TopologicalOrderIterator<Description,
	 * DefaultEdge> iterator =new TopologicalOrderIterator<Description,
	 * DefaultEdge>(modifiedGraph);
	 * 
	 * while (iterator.hasNext()) { Description vertex= iterator.next();
	 * Set<DefaultEdge> edges = modifiedGraph.outgoingEdgesOf(vertex); for
	 * (DefaultEdge edge : edges) { for (DefaultEdge edge2 : edges) {
	 * if(edge.equals(edge2)) continue;
	 * if(isReachable(modifiedGraph.getEdgeTarget(edge),
	 * modifiedGraph.getEdgeTarget(edge2)))
	 * 
	 * redundantEdges.add(edge2); } } }
	 * 
	 * 
	 * modifiedGraph.removeAllEdges(redundantEdges);
	 * 
	 * 
	 * 
	 * }
	 * 
	 * boolean isReachable(Description node, Description vertex){
	 * BreadthFirstIterator<Description, DefaultEdge> iterator= new
	 * BreadthFirstIterator<Description, DefaultEdge>(modifiedGraph, node);
	 * while(iterator.hasNext()){ Description parent=iterator.next();
	 * if(parent.equals(vertex)) return true; } return false; }
	 */

}
