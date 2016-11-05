package main;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import definition.Definition;
import definition.DefinitionReader;
import definition.ProcessedDefinition;
import edu.stanford.nlp.util.Triple;
import utils.DisambiguateUtils.Disambiguation;
import utils.FileUtils;
import utils.ParseUtils;
import utils.ParseUtils.Dependency;

/**
 * DefIE complete pipeline.
 * 
 * @author claudio
 */
@SuppressWarnings("unused")
public class PipelineMain
{
	// Graph construction
	private GraphBuilder gb;
	// Pattern extraction
	private RelationBuilder rb;
	// Relation refinement
	private RelationHandler rh;
	
	/**
	 * Implementation of a {@link DefinitionReader} for the experimental evaluation.
	 */
	public static class BabelNetGlossesReader implements DefinitionReader
	{
		// Loaded glosses
		private List<ProcessedDefinition> glosses;
		
		public BabelNetGlossesReader()
		{
			glosses = Lists.newArrayList();
		}
		
		public BabelNetGlossesReader(String parsedGlossesFile, String disambiguatedGlossesFile)
		{
			this();
			
			// Read parsed glosses (CoNLL format)
			Map<Definition,List<Dependency>> parsedMap = Maps.newHashMap();
			List<String> currentFile = Lists.newArrayList();
			try (Stream<String> lines = Files.lines(Paths.get(parsedGlossesFile), Charset.defaultCharset()))
			{
				lines.map(String::trim).forEach(line ->
				{
					if(line.startsWith("bn:"))
					{
						// Textual content of the definition
						String content = "";
						try {
							content = currentFile.stream().map(token -> token.split("\\t")[1]).collect(Collectors.joining(" "));
						} catch(ArrayIndexOutOfBoundsException e) {
							System.err.println(line);
							System.err.println(currentFile.toString());
							e.printStackTrace();
							System.exit(1);
						}
						// Parse dependencies
						List<Dependency> deps = ParseUtils.parseCoNLLFormat(currentFile);
						// Store definition 
						parsedMap.put(new Definition(line, content), ParseUtils.fixDependencies(deps));
						// Empty current container
						currentFile.clear();
					}
					else currentFile.add(line);
				});
			}
			catch (IOException e)
			{
				System.err.println("[ "+this.getClass().getSimpleName()+" ] Unable to load data from '" + parsedGlossesFile + "'!");
				e.printStackTrace();
				System.exit(1);
			}
			
			// Read disambiguated glosses
			Map<String,List<String>> disambiguatedMap = Maps.newHashMap();
			try (Stream<String> lines = Files.lines(Paths.get(disambiguatedGlossesFile), Charset.defaultCharset()))
			{
				lines.map(String::trim).map(line -> line.split("\\t")).forEach(lineToks ->
					disambiguatedMap.put(lineToks[0].split(" ")[0], Arrays.stream(lineToks).skip(1).collect(Collectors.toList())));
			} catch (IOException e) {
				System.err.println("[ "+this.getClass().getSimpleName()+" ] Unable to load data from '" + disambiguatedGlossesFile + "'!");
				e.printStackTrace();
				System.exit(1);
			}
			
			// Match parses and disambiguations
			parsedMap.keySet().stream().map(definition ->
			{
				// Grab BabelNet ID
				String babelnetID = definition.getID().split("_")[0];
				// Grab definition text
				List<String> textToken = Arrays.asList(definition.getText().split(" ")); 
				// Instanciate disambiguation
				List<Disambiguation> disambiguations = disambiguatedMap.get(babelnetID).stream().filter(disambiguation -> disambiguation.split(" ")[2].equals("EN"))
						.flatMap(disambiguation ->
						{
							String id = disambiguation.split(" ")[0];
							String[] mention = disambiguation.split(" ")[1].split("_");
							double confidence = Double.parseDouble(disambiguation.split(" ")[5]);
							
							return textToken.stream().filter(token -> token.equals(mention[0]))
									.filter(token -> matchMultiWord(textToken.subList(textToken.indexOf(token), textToken.size()), mention))
									.map(token -> new Disambiguation(new Triple<>(textToken.indexOf(token), textToken.indexOf(token)+mention.length, id), confidence));
						})
						.collect(Collectors.toList());
				
				return new ProcessedDefinition(definition, parsedMap.get(definition), disambiguations);
			})
			.forEach(glosses::add);
		}
		
		private boolean matchMultiWord(List<String> token, String[] mention)
		{
			if(token.size() >= mention.length) return false;
			else
				return IntStream.range(0,token.size()).mapToObj(i -> token.get(i).equals(mention[i])).reduce(true, (i1,i2) -> i1 && i2).booleanValue();
		}
		
		@Override
		public Iterator<ProcessedDefinition> iterator()
		{
			return glosses.iterator();
		}
		
		@Override
		public List<? extends Definition> readDefinitions(String path)
		{
			glosses = FileUtils.loadRawObjectFromFile(path, false);
			return glosses;
		}
	}
	
	public static void main(String[] args)
	{
		String path = "/home/claudio/Glosses/BabelNet 3.0";
		
		new BabelNetGlossesReader(path+"/parsedGlosses_bn3.0_all.3.en", path+"/full_march16_babelfied_glosses.txt").forEach(System.out::println);
	}
}
