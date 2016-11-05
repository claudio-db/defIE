package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Some utility functions for writing and reading object to and from the file system.
 * 
 * @author claudio
 */
public class FileUtils {

	/**
	 * Reads a string-to-string map from a file in .tsv format.
	 * 
	 * @param input_file Path to .tsv file
	 * @param verbose Verbose flag
	 */
	public static Map<String,String> loadStringMapFromTSVfile(String input_file, boolean verbose)
	{
		Map<String,String> result = new HashMap<String,String>();
		try
		{
			// Open stream			
			if(verbose) System.out.println("[ FileUtils ] Reading map from '"+input_file+"'... ");
			FileInputStream source = new FileInputStream(input_file);
			Scanner reader = new Scanner(source);
			
			// Read file line-by-line
			int line_count = 1;
			while(reader.hasNextLine())
			{
				String line = reader.nextLine();
				try {
					result.put(line.split("\\t")[0], line.split("\\t")[1]);
				} catch(NullPointerException e) {
					if(verbose)
						System.err.println("[ FileUtils ] ERROR! Parsing at line"+line_count+": "+line);
				}
				++line_count;
			}
			
			// Clean everything up
			reader.close();
			source.close();
		}
		catch (IOException e)
		{
			if(verbose) System.err.println("[ FileUtils ] ERROR! Unable to read '"+input_file+"':");
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Reads a integer-to-integer map from a file in .tsv format.
	 * 
	 * @param input_file Path to .tsv file
	 * @param verbose Verbose flag
	 */
	public static Map<Integer,Integer> loadIntMapFromTSVfile(String input_file, boolean verbose)
	{
		Map<Integer,Integer> result = new HashMap<Integer,Integer>();
		try
		{
			// Open stream			
			if(verbose) System.out.println("[ FileUtils ] Reading map from '"+input_file+"'... ");
			FileInputStream source = new FileInputStream(input_file);
			Scanner reader = new Scanner(source);
			
			// Read file line-by-line
			int line_count = 1;
			while(reader.hasNextLine())
			{
				String line = reader.nextLine();
				try {
					result.put(Integer.parseInt(line.split("\\t")[0]), Integer.parseInt(line.split("\\t")[1]));
				} catch(NullPointerException e) {
					if(verbose)
						System.err.println("[ FileUtils ] ERROR! Parsing at line"+line_count+": "+line);
				}
				++line_count;
			}
			
			// Clean everything up
			reader.close();
			source.close();
		}
		catch (IOException e)
		{
			if(verbose) System.err.println("[ FileUtils ] ERROR! Unable to read '"+input_file+"':");
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Reads a integer-to-string map from a file in .tsv format.
	 * 
	 * @param input_file Path to .tsv file
	 * @param verbose Verbose flag
	 */	
	public static Map<Integer,String> loadInt2StringMapFromTSVfile(String input_file, boolean verbose)
	{
		Map<Integer,String> result = new HashMap<Integer,String>();
		try
		{
			// Open stream			
			if(verbose) System.out.println("[ FileUtils] Reading map from '"+input_file+"'... ");
			FileInputStream source = new FileInputStream(input_file);
			Scanner reader = new Scanner(source);
			
			// Read file line-by-line
			int line_count = 1;
			while(reader.hasNextLine())
			{
				String line = reader.nextLine();
				try {
					result.put(Integer.parseInt(line.split("\\t")[0]), line.split("\\t")[1]);
				} catch(NullPointerException e) {
					if(verbose)
						System.err.println("[ FileUtils ] ERROR! Parsing at line"+line_count+": "+line);
				}
				++line_count;
			}
			
			// Clean everything up
			reader.close();
			source.close();
		}
		catch (IOException e)
		{
			if(verbose) System.err.println("[ FileUtils ] ERROR! Unable to read '"+input_file+"':");
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Reads a string-to-integer map from a file in .tsv format.
	 * 
	 * @param input_file Path to .tsv file
	 * @param verbose Verbose flag
	 */	
	public static Map<String,Integer> loadString2IntMapFromTSVfile(String input_file, boolean verbose)
	{
		Map<String,Integer> result = new HashMap<String,Integer>();
		try
		{
			// Open stream			
			if(verbose) System.out.println("[ FileUtils] Reading map from '"+input_file+"'... ");
			FileInputStream source = new FileInputStream(input_file);
			Scanner reader = new Scanner(source);
			
			// Read file line-by-line
			int line_count = 1;
			while(reader.hasNextLine())
			{
				String line = reader.nextLine();
				try {
					result.put(line.split("\\t")[0], Integer.parseInt(line.split("\\t")[1]));
				} catch(NullPointerException e) {
					if(verbose)
						System.err.println("[ FileUtils ] ERROR! Parsing at line"+line_count+": "+line);
				}
				++line_count;
			}
			
			// Clean everything up
			reader.close();
			source.close();
		}
		catch (IOException e)
		{
			if(verbose) System.err.println("[ FileUtils ] ERROR! Unable to read '"+input_file+"':");
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Given two maps where values in the first are keys in the second, creates a third map applying transitivity.
	 * 
	 * @param map_i First map
	 * @param map_j Second map
	 * @param verbose Verbose flag
	 */
	public static <T1, T2, T3> Map<T1,T3> createTransitiveMap(Map<T1,T2> map_i, Map<T2,T3> map_j, boolean verbose)
	{
		Map<T1,T3> result = new HashMap<T1,T3>();
		for(T1 key_i : map_i.keySet())
			if(map_j.get(map_i.get(key_i)) != null)
				result.put(key_i, map_j.get(map_i.get(key_i)));
			else if (verbose)
				System.err.println("[ FileUtils ] WARNING: missing key value for "+key_i+" in the second map...");
		
		return result;
	}
	
	/**
	 * Load a raw serialized object from the file system.
	 * 
	 * @param input Path to raw object
	 * @param verbose Verbose flag
	 */
	@SuppressWarnings("unchecked")
	public static <T> T loadRawObjectFromFile(String input, boolean verbose)
	{		
		T object = null;
		if(verbose) System.out.print("[ FileUtils ] Reading raw object from '"+input+"'... ");
		long startTime = System.currentTimeMillis();
	
		try {
			// Create stream
			InputStream file = new FileInputStream(input);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInputStream input_stream = new ObjectInputStream(buffer);
			
			// Read object
			object = (T) input_stream.readUnshared();
			//object = (T) input_stream.readObject();
						
			// Close everything
			input_stream.close();
			file.close();
			buffer.close();
			
			if(verbose) System.out.println("Done. ["+((System.currentTimeMillis()-startTime))+" ms ]");
			return object;
		}
		catch (Exception e)
		{
			if(verbose) System.err.println("\n[ FileUtils ] ERROR: Unable to read '"+input+"':");
			else System.err.println("[ FileUtils ] ERROR: Unable to read '"+input+"':");
			e.printStackTrace();
			
			return object;
		}
	}

	/**
	 * Write a raw serialized object to the file system.
	 * 
	 * @param object The object to be written
	 * @param output_file Destination path
	 * @param verbose Verbose flag
	 */
	public static <T> void writeObjectToRawFile(T object, String output_file, boolean verbose)
	{
		if(verbose) System.out.print("[ FileUtils ] Writing raw object to '"+output_file+"'... ");
		long startTime = System.currentTimeMillis();
		
		try {
			// Open output stream
			OutputStream file = new FileOutputStream(output_file);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutputStream output = new ObjectOutputStream(buffer);
			
			// Write object
			output.writeUnshared(object);
			//output.writeObject(object);
			
			// Do not keep a reference for the created object
			output.reset();
			
			// Clean up everything
			output.close();
			buffer.close();
			file.close();
			
			if(verbose) System.out.println("Done. ["+((System.currentTimeMillis()-startTime))+" ms ]");
		}
		catch (Exception e)
		{
			if(verbose) System.err.println("\n[ FileUtils ] ERROR: Unable to write '"+output_file+"':");
			else System.err.println("[ FileUtils ] ERROR: Unable to write '"+output_file+"':");
			e.printStackTrace();
			
			System.exit(1);
		}
	}
	
	/**
	 * Write a list of raw serialized objects to the file system.
	 * 
	 * @param object The list of objects to be written
	 * @param output_file Destination path
	 * @param split Size of individual split 
	 * @param verbose Verbose flag
	 */
	public static <T> void writeObjectListToRawFile(List<T> object, String output_file, int split, boolean verbose)
	{
		int n_slice = 1;
		int done_index = 0;
		
		while (done_index < object.size())
		{
			// Fill current slice
			List<T> current = new ArrayList<T>(object.subList(done_index, Math.min(done_index+split, object.size())));
			done_index += split;
			
			// Current filename
			StringBuilder current_output_file = new StringBuilder();
			current_output_file.append(output_file).append(".part").append(n_slice);
			
			if(verbose) System.out.print("[ FileUtils ] Writing raw object to '"+current_output_file.toString()+"'... ");
			long startTime = System.currentTimeMillis();
			try {
				// Open output stream
				OutputStream file = new FileOutputStream(current_output_file.toString());
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutputStream output = new ObjectOutputStream(buffer);
				
				// Write object
				output.writeUnshared(current);
				//output.writeObject(current);
				
				// Do not keep a reference for the created object
				output.reset();
				
				// Clean up everything
				output.close();
				buffer.close();
				file.close();
				
				if(verbose) System.out.println("Done. ["+((System.currentTimeMillis()-startTime))+" ms ]");
			}
			catch (Exception e)
			{
				if(verbose) System.err.println("\n[ FileUtils ] ERROR: Unable to write '"+current_output_file.toString()+"':");
				else System.err.println("[ FileUtils ] ERROR: Unable to write '"+current_output_file.toString()+"':");
				e.printStackTrace();
				
				System.exit(1);
			}
			
			++n_slice;
		}
	}

}
