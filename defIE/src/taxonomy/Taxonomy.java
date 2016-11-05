package taxonomy;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Generic class that represents a taxonomy.
 * Allows for non-fully connected taxonomies (i.e. forests) and can be iterated over the set of roots.
 * {@link Edge}s of the {@link Taxonomy} have to be comparable.
 * 
 * @author claudio
 * @param <T> Node type
 * @param <E> Edge type
 */
public class Taxonomy<T,E extends Comparable<? super E>> implements Iterable<T>
{

	// Hypernymy edges
	private final Map<T,SortedSet<Edge>> edgeMap;
	// Roots of the taxonomy
	private final Set<T> roots;
	
	// Implementation of the edge list
	private final Supplier<SortedSet<Edge>> edgeListSupplier = TreeSet::new;

	/**
	 * Hidden constructor.
	 * Initialize the {@link Taxonomy} and the set of roots.
	 * 
	 * @param edgeMap Nodes and edges of the {@link Taxonomy}
	 */
	protected Taxonomy(Map<T,Map<T,E>> edgeMap)
	{
		this.edgeMap = Maps.newHashMap();
		edgeMap.forEach((node,map) ->
		{
			this.edgeMap.putIfAbsent(node, edgeListSupplier.get());
			map.entrySet().stream().map(e -> new Edge(e.getKey(),e.getValue())).forEach(this.edgeMap.get(node)::add);
		});
		// Find roots
		this.roots = this.edgeMap.values().stream().flatMap(set -> set.stream()).map(Edge::target)
				.filter(id -> !edgeMap.containsKey(id)).collect(Collectors.toSet());
	}

	/**
	 * Get all {@link Edge}s associated with a given node.
	 * 
	 * @param node Input node
	 * @return A collection of {@link Edge}s to superclasses of node
	 */
	public Collection<Edge> getAllEdges(T node)
	{
		return edgeMap.getOrDefault(node, edgeListSupplier.get());
	}
	
	/**
	 * Get all {@link Edge}s associated with a given node that satisfy the provided filter.
	 * 
	 * @param node Input node
	 * @param edgeFilter Filter for {@link Edge}s
	 * @return A collection of {@link Edge}s to superclasses of node
	 */	
	public Collection<Edge> getEdges(T node, Predicate<Edge> edgeFilter)
	{
		return getAllEdges(node).stream().filter(edgeFilter).collect(Collectors.toList());
	}
	
	/**
	 * Get all the superclasses associated with a given node.
	 * 
	 * @param node Input node
	 * @return A collection of superclasses
	 */
	public Collection<T> getSuperclasses(T node)
	{
		return getAllEdges(node).stream().map(Edge::target).collect(Collectors.toList());
	}
	
	/**
	 * Get the superclass associated with a given node.
	 * If more than one superclass is provided, the top one (according to the {@link Edge} type) is selected.
	 * 
	 * @param node Input node
	 * @return Corresponding superclass
	 */
	public Optional<T> getSuperclass(T node)
	{
		return getAllEdges(node).stream().findFirst().map(Edge::target);
	}

	/**
	 * Total number of {@link Edge}s in the {@link Taxonomy}.
	 */
	public final long numberOfEdges()
	{
		return edgeMap.values().stream().mapToInt(SortedSet::size).sum();
	}
	
	/**
	 * Total number of @link Edge}s in the {@link Taxonomy} matching a given filter.
	 * 
	 * @param filter {@link Predicate} over {@link Edge}s
	 */
	public final long numberOfEdges(Predicate<Edge> filter)
	{
		return edgeMap.values().stream().flatMap(SortedSet::stream).filter(filter).count();
	}
	
	/**
	 * Check whether two given nodes are superclass of one another.
	 * 
	 * @param node Candidate subclass
	 * @param superclass Candidate superclass
	 * @return 'true' only if the second argument is a superclass of the first one
	 */
	public boolean hasSuperclass(T node, T superclass)
	{
		// Base case #1: the first node is a root
		if(getAllEdges(node).isEmpty())
			return false;
		// Base case #2: (positive) there is a direct edge from the first to the second node
		else if(!getEdges(node, edge -> edge.target().equals(superclass)).isEmpty())
			return true;
		// Recursive case
		else
			return getAllEdges(node).stream().map(Edge::target).anyMatch(fatherNode -> hasSuperclass(fatherNode, superclass));
	}
	
	/**
	 * Return the chain of superclasses from a given node up to the root.
	 * 
	 * @param node Input node
	 * @return list of superclasses of the input node
	 */
	public List<T> taxonomize(T node)
	{
		return taxonomize(node, Integer.MAX_VALUE);
	}

	/**
	 * Return the chain of superclasses from a given node up to the root.
	 * 
	 * @param node Input node
	 * @param maxLevel Maximum number of hops
	 * @return list of superclasses of the input node
	 */
	public List<T> taxonomize(T node, int maxLevel)
	{
		List<T> chain = Lists.newLinkedList();
		if(!roots.contains(node) && (maxLevel > 0))
			getSuperclass(node).ifPresent(superclass ->
			{
				chain.add(superclass);
				taxonomize(superclass, chain, maxLevel-1);
			});
		return chain;
	}
	
	private List<T> taxonomize(T node, List<T> partial, int threshold)
	{
		Optional<T> next = getSuperclass(node);
		if(!roots.contains(node) && (threshold > 0))
			next.filter(superclass -> !partial.contains(superclass)).ifPresent(superclass ->
			{
				partial.add(superclass);
				taxonomize(superclass, partial, threshold-1);
			});
		return partial;		
	}
	
	/**
	 * Return the depth of a given node in the {@link Taxonomy}.
	 * If the provided node is not part of the {@link Taxonomy} the method return -1.
	 * 
	 * @param node Input node
	 * @return depth of node the {@link Taxonomy} (0 if node is a root)
	 */
	public int depthOf(T node)
	{
		return edgeMap.containsKey(node)? taxonomize(node).size() : -1;
	}
	
	@Override
	public Iterator<T> iterator()
	{
		return edgeMap.keySet().iterator();
	}
	
	/**
	 * Generic inner class representing an {@link Edge} of the {@link Taxonomy}.
	 * Encodes information about the target and the edge type, which should be {@link Comparable}.
	 * 
	 * @author claudio
	 */
	public class Edge implements Comparable<Edge>
	{
		// Edge target
		private final T target;
		// Edge type or description
		private final E edgeType;
		
		/**
		 * Constructor.
		 * 
		 * @param target Edge target (i.e. superclass)
		 * @param edgeType Type of the edge
		 */
		public Edge(T target, E edgeType)
		{
			this.target = target;
			this.edgeType = edgeType;
		}
		
		public T target()
		{
			return target;
		}
		
		public E type()
		{
			return edgeType;
		}
				
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((edgeType == null) ? 0 : edgeType.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			
			@SuppressWarnings("unchecked")
			Edge other = (Edge) obj;		
			if (!getOuterType().equals(other.getOuterType())) return false;
			
			return edgeType.equals(other.edgeType) && target.equals(other.target);
		}

		@Override
		public int compareTo(Taxonomy<T,E>.Edge o)
		{
			return edgeType.compareTo(o.edgeType);
		}

		private Taxonomy<T,E> getOuterType()
		{
			return Taxonomy.this;
		}
	
		public String toString()
		{
			return target.toString()+"_"+edgeType.toString();
		}
	}
}
