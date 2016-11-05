package pattern;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;

import com.google.common.collect.Maps;

import definition.Definition;
import edu.stanford.nlp.util.Pair;
import graph.Edge;
import graph.Graph.UndirectedPath;
import graph.Node;
import graph.SemanticNode;
import graph.SynSemGraph;
import utils.DisambiguateUtils.Disambiguation;
import utils.ParseUtils;
import utils.ParseUtils.Token;

/**
 * Class modeling a pattern in the {@link SynSemGraph}, including starting and ending nodes.
 * It uses the generic classes {@link Node}s and {@link Edge}s to encode graph components from an extracted {@link GraphPath}.
 * It carries explicit information about the {@link SemanticNode}s within the patterns.
 * 
 * @author claudio
 */
public class Pattern implements Serializable, Comparable<Pattern>
{
	private static final long serialVersionUID = 3016107999473548586L;
	
	// Relation pattern
	protected final RelationPattern pattern;
	// Starting and ending nodes
	protected final Node startingNode, endingNode;
	// Source definition
	protected final Definition source;
	
	// String signatures
	protected String signatureWithArguments, signatureLemmatizedWithArguments;
	// Node placeholder for the arguments
	public static final String leftPlaceholder = "X";
	public static final String rightPlaceholder = "Y";
	
	// Predicates on the graph paths
	public static final Predicate<Pattern> NOMINAL_ARGUMENTS = p ->
			ParseUtils.IS_NOUN.test(p.getExtremes().first().getToken()) && ParseUtils.IS_NOUN.test(p.getExtremes().second().getToken());
	public static final Predicate<Pattern> ENDS_WITH_MODIFIER = p ->
			p.getEdgePath().get(p.getEdgePath().size()-1).type().matches("nn") || p.getEdgePath().get(p.getEdgePath().size()-1).type().matches("ncmod");
	public static final Predicate<Pattern> STARTS_WITH_MODIFIER = p ->
			p.getEdgePath().get(0).type().matches("nn") || p.getEdgePath().get(0).type().matches("ncmod");
	
	/**
	 * Constructor.
	 * 
	 * @param path List of {@link Edge}s encoding the actual path in the {@link SynSemGraph}
	 * @param source Source {@link Definition} of the extraction
	 * @throws InvalidPatternException 
	 */
	public Pattern(UndirectedPath path, Definition source)
			throws InvalidPatternException
	{
		// Initialize private members
		this.startingNode = path.getStartingEdge();
		this.endingNode = path.getEndingNode();
		this.pattern = new RelationPattern(path);
		this.source = source;
		this.signatureWithArguments = "";
		this.signatureLemmatizedWithArguments = "";
	}
		
	/**
	 * Getter for the actual {@link RelationPattern}.
	 */
	public RelationPattern getPattern()
	{
		return pattern;
	}
	
	/**
	 * Getter for the actual sequence of {@link Edge}s.
	 */
	public List<Edge> getEdgePath()
	{
		return getPattern().getEdgePath();
	}
	
	/**
	 * Getter for the extreme {@link Node}s of this {@link Pattern}.
	 * The starting node (left extreme) is the first element, the ending node (right extreme) is the second element.
	 * 
	 * @return A {@link Pair} of {@link Node}s
	 */
	public Pair<Node,Node> getExtremes()
	{
		return new Pair<>(startingNode, endingNode);
	}
	
	/**
	 * Getter for the leading verb (or copula) of the {@link Pattern}.
	 */
	public Token getVerb()
	{
		return getPattern().getVerb();
	}
	
	/**
	 * Getter for the source {@link Definition} where the {@link Pattern} was extracted.
	 */
	public Definition getSource()
	{
		return source;
	}
	
	public int patternLength()
	{
		return getPattern().patternLength();
	}

	/**
	 * Compute the disambiguation score of the argument pair.
	 * This score is just the product of the individual scores associated with each {@link Disambiguation}.
	 * 
	 * @return The argument score as double
	 */
	public double argumentScore()
	{
		return ((SemanticNode)startingNode).getSenseAttachment().getConfidence() *
				((SemanticNode)endingNode).getSenseAttachment().getConfidence();
	}

	/**
	 * Compute the disambiguation score of the {@link Pattern}.
	 * This score is just the product of the individual scores associated with each {@link SemanticNode} across the {@link Pattern}.
	 * 
	 * @return The pattern score as double
	 */
	public double patternScore()
	{
		return getPattern().semanticNodes.stream()
				.map(SemanticNode::getSenseAttachment).mapToDouble(Disambiguation::getConfidence)
				.reduce(1.0, (s1,s2) -> s1 * s2);
	}
	
	@Override
	public int compareTo(Pattern o)
	{
		if(equals(o)) return -1;
		else return pattern.compareTo(o.getPattern());
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((endingNode == null) ? 0 : endingNode.hashCode());
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((startingNode == null) ? 0 : startingNode.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Pattern other = (Pattern) obj;
		return pattern.equals(other.pattern) && startingNode.equals(other.startingNode)
				&& endingNode.equals(endingNode) && source.equals(other.source);
	}

	/**
	 * Retrieves the sequence of {@link Node}s sorted by occurrence in the sentence.
	 * 
	 * @param arguments Whether to include starting and ending {@link Node}s
	 * @return The sequence as list of {@link Node}s
	 */
	private List<Node> sortedNodeSequence(boolean arguments)
	{
		// Create sorted sequence of tokens
		TreeMap<Integer,Node> nodeSequence = Maps.newTreeMap();
		nodeSequence.put(startingNode.getToken().index(), arguments?
				startingNode : new Node(new Token(leftPlaceholder, leftPlaceholder, leftPlaceholder, startingNode.getToken().index())));
		Node current = startingNode;
		
		// Arrange the sequence correctly regardless of the edge orientation
		for(Edge e : getPattern().getEdgePath())
		{
			// Edge directed towards the next token
			if(current.equals(e.getSource())) {
				Node currentNode = (e.getTarget().equals(endingNode) && !arguments)?
						new Node(new Token(rightPlaceholder, rightPlaceholder, rightPlaceholder, endingNode.getToken().index())) : e.getTarget();
				nodeSequence.put(currentNode.getToken().index(), currentNode);
				current = e.getTarget();
			}
			// Edge directed towards the current token
			else {
				Node currentNode = (e.getSource().equals(endingNode) && !arguments)?
						new Node(new Token(rightPlaceholder, rightPlaceholder, rightPlaceholder, endingNode.getToken().index())) : e.getSource();
				nodeSequence.put(currentNode.getToken().index(), currentNode);
				current = e.getSource();
			}
		}
		
		return nodeSequence.values().stream().collect(Collectors.toList());
	}
	
	/**
	 * Compute a string representation of the {@link Pattern}.
	 * Each token in the pattern is concatenated according to the order they appear within the sentence.
	 * 
	 * @param lemmatized 'true' if token have to be lemmatized
	 * @param arguments 'true' if the arguments' tokens have to be included (otherwise they are replaced by X and Y)
	 */
	private String computeSignature(boolean lemmatized, boolean arguments)
	{

		// Construct the string signature
		String newSignature = sortedNodeSequence(arguments).stream()
				.map(node -> {
					// In case of a semantic node, print it as a whole
					if(node instanceof SemanticNode)
						return node.toString();
					// In case of a regular node, print either the lemma or surface form
					else
						return lemmatized? node.getToken().lemma() : node.getToken().surfaceForm();
				})
				.collect(Collectors.joining(" "));
		
		if(lemmatized && arguments)
		{
			this.signatureLemmatizedWithArguments = newSignature;
			return this.signatureLemmatizedWithArguments;
		}
		else if(lemmatized)
		{
			this.getPattern().signatureLemmatized = newSignature;
			return this.getPattern().signatureLemmatized;
		}
		else if(arguments)
		{
			this.signatureWithArguments = newSignature;
			return signatureWithArguments;
		}
		else 
		{
			this.getPattern().signature = newSignature;
			return this.getPattern().signature;
		}
	}
	
	public String toString()
	{
		return getPattern().toString();
	}
	
	public String toStringLemmatized()
	{
		return getPattern().toStringLemmatized();
	}
	
	public String toStringWithArguments()
	{
		return signatureWithArguments.isEmpty()? computeSignature(false, true) : signatureWithArguments;
	}
	
	public String toStringLemmatizedWithArguments()
	{
		return signatureLemmatizedWithArguments.isEmpty()? computeSignature(true, true) : signatureLemmatizedWithArguments;
	}
	
	public String toStringVerbose(boolean arguments)
	{
		return sortedNodeSequence(arguments).stream().map(Node::toStringVerbose).collect(Collectors.joining(" "));
	}

	public class RelationPattern implements Serializable, Comparable<RelationPattern>
	{
		private static final long serialVersionUID = 4275643111012231171L;
		
		// Path in the syntactic-semantic graph
		protected final List<Edge> path;
		// Pattern as token sequence
		protected List<Node> pathNodes;
		// Argument indices
		protected int leftArgumentIndex, rightArgumentIndex;
		
		// Leading verb (or copula) node in the pattern
		protected Token verb;
		// Semantic nodes in the pattern
		protected List<SemanticNode> semanticNodes;
		
		// String signatures
		protected String signature, signatureLemmatized;
		
		/**
		 * Constructor.
		 * 
		 * @param path List of {@link Edge}s encoding the actual path in the {@link SynSemGraph}
		 * @throws InvalidPatternException
		 */
		public RelationPattern(UndirectedPath path)
				throws InvalidPatternException
		{
			// Initialize private members
			this.path = path.getEdges();
			this.signature = "";
			this.signatureLemmatized = "";
			
			// Identify leading verb
			this.verb = this.path.stream().map(Edge::getTarget)
					.map(Node::getToken).filter(ParseUtils.IS_VERB)
					.findFirst().orElseThrow(() -> new InvalidPatternException("No verb node found!", path.getEdges()));
			
			// Collect semantic nodes across the path
			semanticNodes = this.path.stream().map(Edge::getTarget)
					.filter(node -> !node.equals(path.getEndingNode()) && (node instanceof SemanticNode))
					.map(node -> (SemanticNode)node).collect(Collectors.toList());
		}
		
		private void initializeNodeIndices()
		{
			// Initialize argument indices
			List<Node> nodeSequenceWithArguments = Pattern.this.sortedNodeSequence(true);
			this.leftArgumentIndex = nodeSequenceWithArguments.indexOf(Pattern.this.startingNode);
			this.rightArgumentIndex = nodeSequenceWithArguments.indexOf(Pattern.this.endingNode);
			this.pathNodes = nodeSequenceWithArguments.stream().collect(Collectors.toList());
		}
	
		/**
		 * Getter for the actual sequence of {@link Edge}s.
		 */
		public List<Edge> getEdgePath()
		{
			return path;
		}
	
		/**
		 * Getter for the sorted list of {@link Token}s in the {@link RelationPattern}.
		 */
		public List<Node> getNodes()
		{
			if(!Optional.ofNullable(pathNodes).isPresent()) initializeNodeIndices();
			return pathNodes;
		}
		
		public List<SemanticNode> getSemanticNodes()
		{
			return semanticNodes;
		}		
		
		/**
		 * Getter for the leading verb (or copula) of the {@link Pattern}.
		 */
		public Token getVerb()
		{
			return verb;
		}
		
		public int patternLength()
		{
			return path.size();
		}
		
		public String toString()
		{
			return signature.isEmpty()? computeSignature(false, false) : signature;
		}
		
		public String toStringLemmatized()
		{
			return signatureLemmatized.isEmpty()? computeSignature(true, false) : signatureLemmatized;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + ((semanticNodes == null) ? 0 : semanticNodes.hashCode());
			result = prime * result + ((signature == null) ? 0 : signature.hashCode());
			result = prime * result + ((signatureLemmatized == null) ? 0 : signatureLemmatized.hashCode());
			result = prime * result + ((verb == null) ? 0 : verb.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			
			RelationPattern other = (RelationPattern) obj;
			return path.equals(other.path);
		}
		
		@Override
		public int compareTo(RelationPattern o)
		{
			if(equals(o)) return -1;
			else return Integer.compare(path.size(), o.path.size());
		}	
	
		public int leftArgumentIndex()
		{
			if(!Optional.ofNullable(leftArgumentIndex).isPresent()) initializeNodeIndices();
			return leftArgumentIndex;
		}
		
		public int rightArgumentIndex()
		{
			if(!Optional.ofNullable(rightArgumentIndex).isPresent()) initializeNodeIndices();
			return rightArgumentIndex;
		}
	}
	
	class InvalidPatternException extends Exception
	{
		private static final long serialVersionUID = 1642129338357438015L;
		
		private final String cause;
		private final List<Edge> path;
		
		public InvalidPatternException(String cause, List<Edge> invalidPath)
		{
			this.cause = cause;
			this.path = invalidPath;
		}
		
		public String getCauseMessage()
		{
			return cause;
		}
		
		public List<Edge> getInvalidPath()
		{
			return path;
		}
	}
}
