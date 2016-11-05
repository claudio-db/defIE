package relation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import utils.DisambiguateUtils;

/**
 * Simple class representing an element of the reference sense inventory.
 * It is associated with a unique identifier and might have more than one lexicalization.
 * 
 * @author claudio
 */
public class Concept implements Serializable
{
	private static final long serialVersionUID = 8827019347232809039L;
	
	// Concept ID
	private final String id;
	// Lexicalizations
	private List<String> lexicalizations;
	
	/**
	 * Constructor #1.
	 * 
	 * @param id Unique identifier for the {@link Concept}
	 * @param lexicalizations One or more lexicalizations
	 */
	public Concept(String id, String... lexicalizations)
	{
		this.id = id;
		this.lexicalizations = Arrays.asList(lexicalizations);
	}
	
	public String id()
	{
		return this.id;
	}
	
	public Collection<String> lexicalizations()
	{
		return lexicalizations;
	}
	
	public void addLexicalization(String newLexicalization)
	{
		lexicalizations.add(newLexicalization);
	}
	
	public String toString()
	{
		return lexicalizations.stream().findFirst().map(lex -> lex+DisambiguateUtils.SEPARATOR).orElse("")+id;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Concept other = (Concept) obj;
		return id.equals(other.id);
	}
}
