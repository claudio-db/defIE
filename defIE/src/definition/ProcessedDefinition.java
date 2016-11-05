package definition;

import java.util.List;

import utils.DisambiguateUtils.Disambiguation;
import utils.ParseUtils.Dependency;

import com.google.common.collect.Lists;

public class ProcessedDefinition extends Definition
{
	private static final long serialVersionUID = -2089084371236925782L;
	
	// Dependencies
	private List<Dependency> dependencies;
	// Sense mappings
	private List<Disambiguation> senses;
	
	/**
	 * Constructor #1.
	 * Initializes a non-processed definition.
	 * 
	 * @param id Definition identifier
	 * @param content Definition text
	 */
	public ProcessedDefinition(String id, String content)
	{
		super(id, content);
		dependencies = Lists.newArrayList();
		senses = Lists.newArrayList();
	}

	/**
	 * Constructor #2.
	 * Initializes a non-processed definition.
	 * 
	 * @param definition Non-processed {@link Definition}
	 */
	public ProcessedDefinition(Definition definition)
	{
		this(definition.getID(), definition.getText());
	}
	
	/**
	 * Constructor #3.
	 * 
	 * @param id Definition identifier
	 * @param content Definition text
	 * @param dependencies List of typed dependencies
	 * @param senses List of sense matchings as {@link Disambiguation}s
	 * 
	 */
	public ProcessedDefinition(String id, String content, List<Dependency> dependencies, List<Disambiguation> senses)
	{
		this(id, content);
		addSyntacticInformation(dependencies);
		addSemanticInformation(senses);
	}
	
	/**
	 * Constructor #4.
	 * 
	 * @param definition Non-processed {@link Definition}
	 * @param dependencies List of typed dependencies
	 * @param senses List of sense matchings as {@link Disambiguation}s
	 */
	public ProcessedDefinition(Definition definition, List<Dependency> dependencies, List<Disambiguation> senses)
	{
		this(definition.getID(), definition.getText(), dependencies, senses);
	}
	
	/**
	 * Add parsing information to the definition.
	 * 
	 * @param dependencies List of typed dependencies
	 */
	public void addSyntacticInformation(List<Dependency> dependencies)
	{
		this.dependencies = dependencies;
	}
	
	/**
	 * Add disambiguated fragments to the definition.
	 * 
	 * @param senses List of sense matchings as {@link Disambiguation}s
	 */
	public void addSemanticInformation(List<Disambiguation> senses)
	{
		this.senses = senses;
	}

	/**
	 * Getter for the list of typed dependencies.
	 */
	public List<Dependency> getDependencies()
	{
		return dependencies;
	}
	
	/**
	 * Getter for the list of disambiguated fragments.
	 */
	public List<Disambiguation> getSenses()
	{
		return senses;
	}
	
	public boolean isParsed()
	{
		return !dependencies.isEmpty();
	}
	
	public boolean isDisambiguated()
	{
		return !senses.isEmpty();
	}
}
