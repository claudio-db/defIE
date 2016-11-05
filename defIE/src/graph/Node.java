package graph;

import java.io.Serializable;

import utils.ParseUtils.Token;

/**
 * Simple class modeling a generic node in the graph.
 * Wraps the {@link Token} class.
 * 
 * @author claudio
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 8821878227193878549L;
	
	// Attached token
	protected Token token;
	
	/**
	 * Constructor.
	 * 
	 * @param token Attached {@link Token}
	 */
	public Node(Token token)
	{
		this.token = token;
	}
	
	/**
	 * Getter for the attached {@link Token}.
	 */
	public Token getToken()
	{
		return token;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Node other = (Node) obj;
		return token.equals(other.getToken());
	}
	
	public String toString()
	{
		return token.toString();
	}
	
	public String toStringVerbose()
	{
		return token.toStringVerbose();
	}
}
