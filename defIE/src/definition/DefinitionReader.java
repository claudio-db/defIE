package definition;

import java.util.List;

/**
 * Interface for a generic reader of {@link Definition}s and {@link ProcessedDefinition}s.
 * 
 * @author claudio
 */
public interface DefinitionReader extends Iterable<ProcessedDefinition>
{
	/**
	 * Read a set of {@link Definition}s from the file system.
	 * 
	 * @param path Path to the definitions
	 * @return A list of {@link Definition}s
	 */
	public List<? extends Definition> readDefinitions(String path);
}
