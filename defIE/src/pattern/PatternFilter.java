package pattern;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

/**
 * Class modeling the filtering of extracted {@link Pattern}s.
 * 
 * @author claudio
 */
public class PatternFilter
{
	// Pipeline of filters to be applied
	private List<Predicate<Pattern>> pipeline = Lists.newLinkedList();

	/**
	 * Add a new filter to the pipeline.
	 * 
	 * @param filter A {@link Predicate} over {@link Pattern}s
	 * @return The updated {@link PatternFilter}
	 */
	public PatternFilter addFilter(Predicate<Pattern> filter)
	{
		pipeline.add(filter);
		return this;
	}
	
	/**
	 * Add a set of filters to the pipeline.
	 * 
	 * @param filters A series of {@link Predicate}s over {@link Pattern}s
	 * @return The updated {@link PatternFilter}
	 */
	public PatternFilter addFilters(@SuppressWarnings("unchecked") Predicate<Pattern>... filters)
	{
		Arrays.stream(filters).forEach(this::addFilter);
		return this;
	}
	
	/**
	 * Clear all filters from the pipeline. 
	 * 
	 * @return The empty {@link PatternFilter}
	 */
	public PatternFilter clear()
	{
		pipeline.clear();
		return this;
	}
	
	/**
	 * Return all filters as a single {@link Predicate} over {@link Pattern}s.
	 */
	public Predicate<Pattern> asPredicate()
	{
		return pipeline.stream().reduce(p -> false, Predicate::or).negate();
	}
	
	/**
	 * Apply all filters to a given set of {@link Pattern}s.
	 * 
	 * @param patterns A collection of {@link Pattern}s
	 * @return Another collection filtered according to the specified {@link Predicate}s
	 */
	public Collection<Pattern> apply(Collection<Pattern> patterns)
	{
		return patterns.stream().filter(this.asPredicate()).collect(Collectors.toList());
	}
	
	/**
	 * Apply all filters to a given set of {@link Pattern}s.
	 * 
	 * @param patterns An array of {@link Pattern}s
	 * @return Another collection filtered according to the specified {@link Predicate}s
	 */
	public Collection<Pattern> apply(Pattern... patterns)
	{
		return apply(Arrays.asList(patterns));
	}
}