// modified by yugecin, see the git history information for details
// if no git history information is available, please refer to https://github.com/yugecin/opsu-dance
package org.newdawn.slick.util;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import yugecin.opsudance.core.Entrypoint;

/**
 * A simple wrapper around resource loading should anyone decide to change
 * their minds how this is meant to work in the future.
 * 
 * @author Kevin Glass
 */
public class ResourceLoader {
	/** The list of locations to be searched */
	private static ArrayList<ResourceLocation> locations = new ArrayList<>();
	
	static {
		locations.add(new ClasspathLocation());
		locations.add(new FileSystemLocation(Entrypoint.workingdir));
	}
	
	/**
	 * Add a location that will be searched for resources
	 * 
	 * @param location The location that will be searched for resoruces
	 */
	public static void addResourceLocation(ResourceLocation location) {
		locations.add(location);
	}
	
	/**
	 * Remove a location that will be no longer be searched for resources
	 * 
	 * @param location The location that will be removed from the search list
	 */
	public static void removeResourceLocation(ResourceLocation location) {
		locations.remove(location);
	}
	
	/**
	 * Remove all the locations, no resources will be found until
	 * new locations have been added
	 */
	public static void removeAllResourceLocations() {
		locations.clear();
	}

	/**
	 * Adds the given location at the first position so the loader will check this
	 * location first.
	 */
	public static void addPrimaryResourceLocation(ResourceLocation location)
	{
		locations.add(0, location);
	}

	/**
	 * Get a resource
	 * 
	 * @param ref The reference to the resource to retrieve
	 * @return A stream from which the resource can be read
	 */
	public static InputStream getResourceAsStream(String ref)
	{
		for (int i=0;i<locations.size();i++) {
			InputStream in = locations.get(i).getResourceAsStream(ref);
			if (in != null) {
				return in;
			}
		}
		throw new RuntimeException("Resource not found: " + ref);
	}
	
	/**
	 * Check if a resource is available from any given resource loader
	 * 
	 * @param ref A reference to the resource that should be checked
	 * @return True if the resource can be located
	 */
	public static boolean resourceExists(String ref)
	{
		for (int i=0;i<locations.size();i++) {
			if (locations.get(i).getResource(ref) != null) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get a resource as a URL
	 * 
	 * @param ref The reference to the resource to retrieve
	 * @return A URL from which the resource can be read
	 */
	public static URL getResource(String ref)
	{
		for (int i=0;i<locations.size();i++) {
			URL url = locations.get(i).getResource(ref);
			if (url != null) {
				return url;
			}
		}
		throw new RuntimeException("Resource not found: " + ref);
	}
}
