package relation;

import java.io.Serializable;
import java.util.Optional;

import definition.Definition;

/**
 * Simple class representing a pair of {@link Concept}s that are arguments of some {@link Relation}.
 * Encodes information about the source {@link Definition} where the triple was extracted.
 * 
 * @author claudio
 */
public class ArgumentPair implements Serializable
{
	private static final long serialVersionUID = 4727026982431284415L;
	
	// Actual argument pair
	private final Concept leftArgument, rightArgument;
	// Source of the extraction
	private final Definition source;
	// Arguments disambiguation confidence
	private double score;
	
	/**
	 * Constructor #1.
	 * 
	 * @param leftArgument {@link Concept} occurring as left argument of the triple
	 * @param rightArgument {@link Concept} occurring as right argument of the triple
	 * @param source Source {@link Definition} where the triple was extracted
	 * @param score Disambiguation confidence associated with the {@link ArgumentPair}
	 */
	public ArgumentPair(Concept leftArgument, Concept rightArgument, Definition source, double score)
	{
		this.leftArgument = leftArgument;
		this.rightArgument = rightArgument;
		this.source = source;
		this.score = score;
	}
	
	/**
	 * Constructor #2.
	 * Initializes an {@link ArgumentPair} not associated with an actual extraction.
	 * 
	 * @param leftArgument {@link Concept} occurring as left argument of the triple
	 * @param rightArgument {@link Concept} occurring as right argument of the triple
	 */
	public ArgumentPair(Concept leftArgument, Concept rightArgument)
	{
		this(leftArgument, rightArgument, null, Double.NaN);
	}
	
	public Concept left()
	{
		return leftArgument;
	}
	
	public Concept right()
	{
		return rightArgument;
	}

	/**
	 * Getter for the extraction source.
	 * Non-empty only if the {@link ArgumentPair} does come from an extraction.
	 * 
	 * @return (optional) source {@link Definition}
	 */
	public Optional<Definition> extractionSource()
	{
		return Optional.ofNullable(source);
	}
	
	/**
	 * Disambiguation confidence associated with the {@link ArgumentPair}.
	 * 
	 * @return confidence score (NaN if the {@link ArgumentPair} has no associated source)
	 */
	public double getDisambiguationConfidence()
	{
		return score;
	}

	public String toString()
	{
		return "<"+leftArgument+"\t"+rightArgument+">";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((leftArgument == null) ? 0 : leftArgument.hashCode());
		result = prime * result + ((rightArgument == null) ? 0 : rightArgument.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ArgumentPair other = (ArgumentPair) obj;
		
		return leftArgument.equals(other.left()) && rightArgument.equals(other.right());
	}
}
