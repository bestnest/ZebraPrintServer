package dict;

public class KeyError extends Exception {

	/**
	 * serialization serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	public KeyError (String key) {
		super(String.format("Key not found: \"%s\"", key));
	}
	
	public KeyError(String key, String errorMessage) {
		super(String.format("'%s' - %s", key, errorMessage));
	}

}
