package dict;

import java.util.ArrayList;
import java.util.regex.Matcher;


/***
 * This class is meant to model a dictionary structure similar to Python. I made it because I was not at all satisfied with hash tables and such. It has a very much Python-inspired toString.
 * The only thing I wasn't able to do was figure out how to use square bracket syntax, e.g. dictionary["key"] = "value".
 * 
 * This dictionary class only supports string keys, but allows lookup of values by an integer dict position.
 * @author nbroyles
 *
 */
public class Dictionary {
	
	private final ArrayList<String> keys;
	private final ArrayList<Object> values;
	
	/*  Constructors  */
	// Empty dict constructor
	public Dictionary() {
		this.keys = new ArrayList<>();
		this.values = new ArrayList<>();
	}
	
	// Keys and values arraylist constructor
	public Dictionary(ArrayList<String> keys, ArrayList<Object> values) {
		this.keys = keys;
		this.values = values;
	}
	
	// copy constructor
	public Dictionary(Dictionary d) {
		this.keys = d.keys;
		this.values = d.values;
	}
	
	
	
	/* Object Methods */
	
	/***
	 * Get the value of a selector from the Dictionary. Selectors are separated by dots(.)
	 * @param selector the selector to lookup
	 * @return the value the selector points to
	 * @throws KeyError if you try anything stupid
	 */
	public Object get(String selector) throws KeyError {
		return this.get(selector, "\\.");
	}
	
	
	/***
	 * Get the value of a selector from the Dictionary.
	 * @param selector the selector to lookup
	 * @param separator the regex to split the selector by
	 * @return the value the selector points to
	 * @throws KeyError if you try anything stupid
	 */
	public Object get(String selector, String separator) throws KeyError {
		String[] path = selector.split(separator, -1);
		Dictionary currentDict = this;
		
		String currentSelector = "";
		for (int i = 0; i < path.length; i++) {
			String key = path[i];
			int index = currentDict.keys.indexOf(key);
			
			if (index == -1) {
				/* This path does not exist in the current dict yet, so return null.
				 * I really don't want to throw an error here because if something doesn't exist in a dict, that's okay.
				 * I just wanna handle it gracefully.
				*/
				return null;
			} else {
				// The path exists in the current dict
				// If we're at the end, return the value
				if (i == path.length -1) {
					return currentDict.values.get(index);
				}
			}
			
			// if we're not at the end of the path, bump the base
			if (i == 0) {
				currentSelector = key;
			} else {
				currentSelector = currentSelector + separator + key;
			}
			
			try {
				currentDict = (Dictionary) this.get(currentSelector, separator);
			} catch (ClassCastException ex ) {
				throw new KeyError(currentSelector, "not a Dictionary! Cannot get attribute '" + path[i + 1] + "'");
			}
		}
		
		// If we made it to this point, the key did not exist in the dictionary.
		return null;
	}
	
	
	/***
	 * Returns the value at a certain integer index of the dictionary
	 * @param index the index of the object to return
	 * @return Object
	 */
	public Object get(int index) {
		return this.values.get(index);
	}
	
	/***
	 * Sets the value of a selector to the value given. If the path in the dictionary doesn't yet exist, a dictionary to hold the path will be created for you.
	 * @param selector the path to put the object under, separated by dots(.)
	 * @param value the object to set
	 * @throws KeyError if the path is non-existent
	 */
	public void set(String selector, Object value) throws KeyError {
		this.set(selector, value, "\\.");
	}
	
	
	/***
	 * Sets the value of a selector to the value given. If the path in the dictionary doesn't yet exist, a dictionary to hold the path will be created for you.
	 * @param selector the path to put the object under
	 * @param value the object to set
	 * @param separator the regex to split the selector with
	 * @throws KeyError  if the path is non-existent
	 */
	public void set(String selector, Object value, String separator) throws KeyError {
		String[] path = selector.split(separator, -1);
		Dictionary currentDict = this;
		
		String currentSelector = "";
		for (int i = 0; i < path.length; i++) {
			String key = path[i];
			int index = currentDict.keys.indexOf(key);
			
			if (index == -1) {
				// This path does not exist in the current dict yet, so create it
				currentDict.keys.add(key);
				// If we're at the end of the path, add the value, otherwise add a new empty dictionary
				if (i == path.length -1) {
					currentDict.values.add(value);
					break;
				} else {
					currentDict.values.add(new Dictionary());
				}
			} else {
				// The path exists in the current dict
				// If we're at the end, add the value
				if (i == path.length -1) {
					currentDict.values.set(index, value);
					break;
				}
			}
			
			// if we're not at the end of the path, bump the currentDict up
			if (i == 0) {
				currentSelector = key;
			} else {
				currentSelector = currentSelector + separator + key;
			}
			
			try {
				currentDict = (Dictionary) this.get(currentSelector, separator);
			} catch (ClassCastException ex ) {
				throw new KeyError(currentSelector, "not a Dictionary! Cannot set attribute '" + path[i + 1] + "'");
			}
		}
	}
	
	/***
	 * Pops an item from the dictionary and returns the value
	 * @param selector the path of the item to pop
	 * @return the value that was at the selector
	 * @throws KeyError if the selector does not exist
	 */
	public Object pop(String selector) throws KeyError {
		String[] path = selector.split("\\.", -1);
		Dictionary currentDict = this;
		
		for (int i = 0; i < path.length; i++) {
			String key = path[i];
			int index = currentDict.keys.indexOf(key);
			
			if (index == -1) {
				// This index path does not exist
				throw new KeyError(selector);
			} else {
				// The path exists in the current dict
				// If we're at the end, remove the value
				if (i == path.length -1) {
					try {
						currentDict.keys.remove(index);
						Object val = currentDict.values.get(index);
						currentDict.values.remove(index);
						return val;
					} catch (java.lang.IndexOutOfBoundsException ex) {
						throw new KeyError(selector);
					}
				}
			}
			// if we're not at the end of the path, bump the base
			currentDict = (Dictionary) this.get(key);
		}
		
		return null;
	}
	
	
	/***
	 * Removes the selector and value at a given selector from the dictionary.
	 * @param selector the selector to remove
	 * @throws KeyError if there is no such selector
	 */
	public void remove(String selector) throws KeyError {
		this.pop(selector);
	}
	
	
	public int length() {
		return this.keys.size();
	}
	
	public ArrayList<String> keys() {
		return this.keys;
	}
	
	public ArrayList<Object> values() {
		return this.values;
	}
	
	
	public String toJSON() {
		StringBuilder json = new StringBuilder("{");
		for (int i = 0; i < this.keys.size(); i++) {
			json.append("\"").append(this.keys.get(i)).append("\"").append(": ");
			
			// Account for NULL objects
			if (this.values.get(i) == null) {
				json.append("null");
			} else {
				if (this.values.get(i).getClass().getName().equals("java.lang.String")) {
					json.append("\"").append(((String) this.values.get(i)).replaceAll("\"", Matcher.quoteReplacement("\\\\\""))).append("\"");
				} else {
					json.append(this.values.get(i).toString());
				}
			}
			
			// Check to see if we should put a comma, basically if there is one after this.
			if (i < this.keys.size() - 1) {
				json.append(", ");
			}
		}
		json.append("}");
		return json.toString();
	}
	
	/***
	 * Returns a string representation of the dictionary
	 */
	public String toString() {
		return this.toJSON();
	}

}
