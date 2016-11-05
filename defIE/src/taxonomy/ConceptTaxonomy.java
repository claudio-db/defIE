package taxonomy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Maps;

import relation.Concept;

/**
 * Singleton class modeling a {@link Taxonomy} of {@link Concept}s.
 * Its edges are categorized in ordinal classes, from most to least reliable.
 * 
 * @author claudio
 */
public class ConceptTaxonomy extends Taxonomy<Concept, Integer>
{
	// Path to the concept taxonomy in the file system
	private static String taxonomyPath;
	// Singleton instance
	private static ConceptTaxonomy taxonomy;
	
	/**
	 * Private constructor.
	 * 
	 * @param edgeMap Nodes and edges of the {@link Taxonomy}
	 */
	private ConceptTaxonomy(Map<Concept, Map<Concept, Integer>> edgeMap)
	{
		super(edgeMap);
	}
	
	/**
	 * Load a map of nodes and edges from the file system.
	 * The file should be formatted with an edge on each line, in the following format:
	 * 
	 * 		CONCEPT_ID	\t	CONCEPT_ID	\t	EDGE_TYPE
	 * 
	 * where CONCEPT_ID is the unique identifier of a concept and EDGE_TYPE is an integer representing the reliability of the edge (0 being the most reliable).
	 * 
	 * @param path Path to the taxonomy
	 * @return The taxonomy as adjacency list of nodes
	 */
	private static Map<Concept, Map<Concept, Integer>> loadMap(String path)
	{
		Map<Concept, Map<Concept, Integer>> edgeMap = Maps.newHashMap();
		try (Stream<String> lines = Files.lines(Paths.get(path), Charset.defaultCharset()).map(String::trim)) {
			lines.map(line -> line.split("\\t")).forEach(lineToks ->
			{
				try {
					// Parse concepts
					Concept subclass = new Concept(lineToks[0]);
					Concept superclass = new Concept(lineToks[1]);
					// FIll map
					edgeMap.putIfAbsent(subclass, Maps.newHashMap());
					edgeMap.get(subclass).put(superclass, Integer.parseInt(lineToks[2]));
				}
				catch(NullPointerException|NumberFormatException e) {
					System.err.println("[ "+ConceptTaxonomy.class.getSimpleName()+" ] ERROR! Unable to parse\n\t'" +
							Arrays.stream(lineToks).collect(Collectors.joining("\t")) + "':");
					e.printStackTrace();
					System.exit(1);
				}
			});
		}
		catch (IOException e) {
			System.err.println("[ "+ConceptTaxonomy.class.getSimpleName()+" ] ERROR: "
					+ (path.isEmpty()? "No path specified!" : "Unable to load taxonomy from '" + path + "'!"));
			e.printStackTrace();
			System.exit(1);
		}
		return edgeMap;
	}

	public static ConceptTaxonomy getInstance()
	{
		if(taxonomy == null) taxonomy = new ConceptTaxonomy(loadMap(taxonomyPath));
		return taxonomy;
	}
	
	/**
	 * Set the path to the {@link ConceptTaxonomy} in the file system.
	 * The file should be formatted with an edge on each line, in the following format:
	 * 
	 * 		CONCEPT_ID	\t	CONCEPT_ID	\t	EDGE_TYPE
	 * 
	 * where CONCEPT_ID is the unique identifier of a concept and EDGE_TYPE is an integer representing the reliability of the edge (0 being the most reliable).
	 * 
	 * @param newPath Path to the taxonomy
	 */
	public static void setTaxonomyPath(String newPath)
	{
		taxonomyPath = newPath;
		if(taxonomy != null) taxonomy = null;
	}

	/**
	 * Unit test.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		ConceptTaxonomy.setTaxonomyPath("../hypernym_map.int.txt");
		ConceptTaxonomy taxonomy = ConceptTaxonomy.getInstance();

		taxonomy.taxonomize(new Concept("bn:00055018n")).stream().map(Concept::toString).forEach(System.out::println);
		System.out.println(taxonomy.depthOf(new Concept("bn:00031027n")));
		System.out.println(taxonomy.depthOf(new Concept("bn:00055018n")));
		System.out.println( taxonomy.hasSuperclass(new Concept("bn:00055018n"), new Concept("bn:00016845n"))? "YES" : "");
	}
}
