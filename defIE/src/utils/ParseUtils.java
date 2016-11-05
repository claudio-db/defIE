package utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.graph.SimpleGraph;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import graph.Edge;
import graph.Node;

/**
 * Some utility functions for handling typed dependencies.
 * 
 * @author claudio
 */
public class ParseUtils
{
	public static final String SEPARATOR = "|";
	public static final Pattern DEP_PATTERN = Pattern.compile("([^\\(]+)\\((.+), (.+)\\)");
	
	public static final Predicate<Token> IS_NOUN = token -> token.partOfSpeech().startsWith("N");
	public static final Predicate<Token> IS_VERB = token -> token.partOfSpeech().startsWith("V");
	
	/**
	 * Auxiliary class to store information about a tagged token.
	 */	
	public static class Token implements Serializable, Comparable<Token>
	{
		private static final long serialVersionUID = 3044635138988263172L;
		
		private final String surfaceForm;
		private final String lemma;
		private final String partOfSpeech;
		private final int index;
		
		/**
		 * Constructor #1.
		 * 
		 * @param r Surface form
		 * @param l Lemmatized form
		 * @param p Part of speech
		 * @param i Token index within sentence
		 */
		public Token(String surfaceForm, String lemma, String partOfSpeech, int index)
		{
			this.surfaceForm = surfaceForm;
			this.lemma = lemma;
			this.partOfSpeech = partOfSpeech;
			this.index = index;
		}
		
		/**
		 * Constructor #2.
		 * 
		 * @param r Surface form
		 * @param l Lemmatized form
		 * @param p Part of speech
		 */
		public Token(String surfaceForm, String lemma, String partOfSpeech)
		{
			this(surfaceForm, lemma, partOfSpeech, 0);
		}
		
		public String surfaceForm()
		{
			return this.surfaceForm;
		}
		
		public String lemma()
		{
			return this.lemma;
		}
		
		public String partOfSpeech()
		{
			return this.partOfSpeech;
		}
		
		public int index()
		{
			return this.index;
		}
		
		@Override
		public String toString()
		{
			return surfaceForm;
		}
		
		public String toStringVerbose()
		{
			return surfaceForm+SEPARATOR+lemma+SEPARATOR+partOfSpeech+SEPARATOR+index;
		}

		@Override
		public int compareTo(Token o)
		{
			return Comparator.comparingInt(ParseUtils.Token::index).thenComparing(ParseUtils.Token::surfaceForm).compare(this, o);
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + index;
			result = prime * result + ((lemma == null) ? 0 : lemma.hashCode());
			result = prime * result + ((partOfSpeech == null) ? 0 : partOfSpeech.hashCode());
			result = prime * result + ((surfaceForm == null) ? 0 : surfaceForm.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			
			Token other = (Token) obj;
			return surfaceForm.equals(other.surfaceForm()) && lemma.equals(other.lemma()) &&
					partOfSpeech.equals(other.partOfSpeech()) && (index == other.index());
		}
	
	}
	
	/**
	 * Auxiliary class to store information about a typed dependency.
	 */
	public static class Dependency implements Serializable
	{		
		private static final long serialVersionUID = -7089337339911492040L;

		public static final Pattern DEP_PATTERN = Pattern.compile("([^\\(]+)\\(([^\\)]+)\\)");
		
		// Type of dependency
		private final String type;
		// Components
		private final Token head;
		private final Token dependent;
		
		/**
		 * Default constructor.
		 * 
		 * @param t Dependency type
		 * @param h Head word
		 * @param d Dependent word
		 */
		public Dependency(String t, Token h, Token d)
		{
			type = t;
			head = h;
			dependent = d;
		}
		
		public String type()
		{
			return type;
		}
		
		public Token head()
		{
			return head;
		}
		
		public Token dependent()
		{
			return dependent;
		}
		
		@Override
		public String toString()
		{
			return type+"("+head+", "+dependent+")";
		}
		
		public String toStringVerbose()
		{
			return type+"("+head.toStringVerbose()+", "+dependent.toStringVerbose()+")";
		}
	}	

	/**
	 * Parse a dependency-parsed, pos-tagged and lemmatized sentence in the CoNLL format.
	 * The expected format is one token per line, as follows:
	 * 
	 * 		INDEX	\t	TOKEN	\t	LEMMA	\t	POS	\t POS_UD	\t	-	\t HEAD_INDEX	\t	DEPENDENCY_TYPE
	 * 
	 * where INDEX is the index of TOKEN in the original sentence, LEMMA is the corresponding lemma,
	 * POS and POS_UD are the Penn Treebank POS-tag and Universal Dependency POS-tag respectively, and the
	 * dependency is DEPENDENCY_TYPE ( HEAD_INDEX , INDEX).
	 * 
	 * @param sentence List of CoNLL-formatted strings
	 * @return The parsed list of {@link Dependency} objects
	 */
	public static List<Dependency> parseCoNLLFormat(List<String> sentence)
	{
		// First pass: generate tokens
		List<Token> tokens = Lists.newArrayList();
		sentence.stream().map(element -> element.split("\\t"))
					     .map(element -> new Token(element[1], element[2], element[4], Integer.parseInt(element[0])))
					     .sorted().forEach(tokens::add);
		// Dummy root token
		Token root = new Token("ROOT", "ROOT", "");
		
		// Second pass: generate dependencies
		return sentence.stream().map(element -> element.split("\\t"))
						 .map(element -> new Dependency(element[7],
								 (Integer.parseInt(element[6])-1) >= 0 ? tokens.get(Integer.parseInt(element[6])-1) : root,
								 (Integer.parseInt(element[0])-1) >= 0 ? tokens.get(Integer.parseInt(element[0])-1) : root))
						 .collect(Collectors.toList());
	}
	
	/**
	 * Parse a dependency-parsed, pos-tagged and lemmatized sentence in a standard Stanford-like dependency format.
	 * The expected format is one sentence per line, as follows:
	 * 
	 * 		TYPE(TOKEN_LEMMA_POS_INDEX, TOKEN_LEMMA_POS_INDEX)	\t		...		\t		TYPE(TOKEN_LEMMA_POS_INDEX, TOKEN_LEMMA_POS_INDEX)
	 * 
	 * where TYPE is a dependency type, and the first sequence of TOKEN_LEMMA_POS_INDEX represents the head token.
	 * 
	 * @param sentence List of formatted sentences (one string per sentence)
	 * @return The parsed list of {@link Dependency} objects
	 */
	public static List<Dependency> parseSingleRowFormat(List<String> sentence)
	{
		return sentence.stream().flatMap(element -> Arrays.stream(element.split("\\t")))
								.map(element -> {
									Matcher depMatcher = DEP_PATTERN.matcher(element);
									if(depMatcher.find())
									{
										String depType = depMatcher.group(1);
										Token headToken = new Token(depMatcher.group(2).split("_")[0],
												depMatcher.group(2).split("_")[1], depMatcher.group(2).split("_")[2],
												Integer.parseInt(depMatcher.group(2).split("_")[3]));
										Token depToken = new Token(depMatcher.group(3).split("_")[0],
												depMatcher.group(3).split("_")[1], depMatcher.group(3).split("_")[2],
												Integer.parseInt(depMatcher.group(3).split("_")[3]));
										return new Dependency(depType, headToken, depToken);
									}
									else {
										System.err.println("[ ParseUtils::parseSingleRowFormat ] ERROR! Unable to parse '"+element+"'.");
										return null;
									}
								})
								.collect(Collectors.toList());
	}
	
	/**
	 * Extract the head from a dependency graph by looking at the {@link Token} with no outgoing {@link Edge}s.
	 * 
	 * @param dependencyGraph {@link SimpleGraph} representing the dependency graph
	 * @return The head {@link Token}
	 */
	public static Token getHead(SimpleGraph<Node,Edge> dependencyGraph)
	{
		return dependencyGraph.vertexSet().stream()
				.sorted(Comparator.comparingInt(n -> n.getToken().index()))
				.filter(token -> dependencyGraph.outDegreeOf(token) == 0).findFirst().map(Node::getToken).get();
	}
	
	/**
	 * Turn a dependency graph into a collection of {@link Token} ordered by precedence inside the original sentence.
	 * 
	 * @param dependencyGraph {@link SimpleGraph} representing the dependency graph
	 * @return Sorted list of {@link Token}
	 */
	public static Collection<Token> sentenceFromGraph(SimpleGraph<Node, Edge> dependencyGraph)
	{
		return dependencyGraph.vertexSet().stream().map(Node::getToken)
				.sorted(Comparator.comparingInt(Token::index))
				.collect(Collectors.toCollection(LinkedList::new));
	}

	/**
	 * Build a dependency graph from a list of {@link Dependency} objects.
	 * 
	 * @param deps List of {@link Dependency} objects
	 * @return Dependency graph as {@link SimpleGraph}
	 */
	public static SimpleGraph<Node,Edge> buildDependencyGraph(List<Dependency> deps)
	{
		SimpleGraph<Node, Edge> graph = new SimpleGraph<Node,Edge>(Edge.class);
		deps.stream().map(dep -> new Edge(dep.type(), new Node(dep.head()), new Node(dep.dependent()))).forEach(edge ->
		{
			// Add vertices
			if(!graph.containsVertex(edge.getSource())) graph.addVertex(edge.getSource());
			if(!graph.containsVertex(edge.getTarget())) graph.addVertex(edge.getTarget());
			// Add edge
			graph.addEdge(edge.getSource(), edge.getTarget(), edge);
		});
		return graph;
	}

	/**
	 * Modify the original dependency graph in order to unroll some collapsed connections.
	 * This will let the connective particle appear in the relation pattern extracted afterwards.
	 * 
	 * For instance:
	 * 
	 * - In the construction with copulas, the nominial root is directly connected to the referent (with no "to be" node in between);
	 * - In constructions with conjunctions, both heads are directly connected together (with no "and", "or" or other particle in between).
	 * 
	 * @param deps Original list of {@link Dependency} objects
	 * @return Updated list of {@link Dependency} objects
	 */
	public static List<Dependency> fixDependencies(List<Dependency> deps)
	{
		// Expand 'cop' edges
		Set<Integer> depsToRemove = Sets.newHashSet();
		Set<Dependency> depsToAdd = Sets.newHashSet();
		deps.stream().filter(dep -> dep.type().equals("cop")).forEach(cop -> {
			// 'cop' goes from a noun to the verb 'to be'
			Token verb = cop.dependent();
			Token noun = cop.head();
			// Look for the referent of that noun
			deps.stream().filter(dep -> dep.type().equals("nsubj") && dep.head().equals(noun)).findFirst().ifPresent(refDep ->
			{
				Token referent = refDep.dependent();
				// Create new 'cop' connecting referent and verb and replace old one
				Dependency newCop = new Dependency("cop", verb, referent);
				depsToAdd.add(newCop);
				depsToRemove.add(deps.indexOf(cop));
				// Create new 'nsubj' connecting noun and verb
				Dependency newNsubj = new Dependency("nsubj", noun, verb);
				depsToAdd.add(newNsubj);
				depsToRemove.add(deps.indexOf(refDep));
			});
		});
		// Expand 'conj' edges
		deps.stream().filter(dep -> dep.type().equals("conj")).forEach(conj ->
		{
			// 'conj'  directly connects the two nouns
			Token noun1 = conj.head();
			Token noun2 = conj.dependent();
			// Look for a 'cc' edge among the outgoing edges of the head noun
			deps.stream().filter(dep -> dep.type().equals("cc") && dep.head().equals(noun1)).findFirst().ifPresent(ccDep ->
			{
				Token conjunction = ccDep.dependent();
				// Create new 'conj' headed by the actual conjunction and replace old one
				Dependency newConj = new Dependency("conj", conjunction, noun2);
				depsToAdd.add(newConj);
				depsToRemove.add(deps.indexOf(conj));
			});
		});
		
		// Create and return updated list
		List<Dependency> updatedDeps = IntStream.range(0,deps.size())
				.filter(i -> !depsToRemove.contains(i)).mapToObj(deps::get).collect(Collectors.toList());
		updatedDeps.addAll(depsToAdd);
		
		return updatedDeps;
	}
}
