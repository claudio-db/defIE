package definition;

import java.io.Serializable;

/**
 * Class modeling a textual definition input to the extraction pipeline.
 * Stores information about the gloss text, ID (if any) and source, as well as the syntactic and semantic information extracted with the first processing step.
 *  
 * @author claudio
 */
public class Definition implements Serializable
{	
	private static final long serialVersionUID = -4098469190282260171L;

	// Definition identifier
	private final String id;
	// Definition textual content
	private final String content;
	
	/**
	 * Constructor.
	 * 
	 * @param id Definition identifier
	 * @param content Definition text
	 */
	public Definition(String id, String content)
	{
		this.id = id;
		this.content = content;
	}
		
	/**
	 * Getter for the ID.
	 */
	public String getID()
	{
		return id;
	}
	
	/***
	 * Getter for the text.
	 */
	public String getText()
	{
		return content;
	}
	
	public boolean isProcessed()
	{
		return isParsed() && isDisambiguated();
	}
	
	public boolean isParsed()
	{
		return false;
	}
	
	public boolean isDisambiguated()
	{
		return false;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Definition other = (Definition) obj;
		return content.equals(other.content) && id.equals(other.id);
	}

	@Override
	public String toString()
	{
		return content;
	}
	
	public String toStringVerbose()
	{
		return id+"\t"+content;
	}
}