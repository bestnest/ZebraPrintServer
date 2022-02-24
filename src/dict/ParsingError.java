package dict;

public class ParsingError extends Exception {

	/**
	 * I don't really know bro, just go.
	 */
	private static final long serialVersionUID = 1L;
	
	public ParsingError(String errorMessage, int position) {
		super(String.format("A parsing error occuring in position %i: %s", position, errorMessage));
	}
	
	public ParsingError(String errorMessage) {
		super(errorMessage);
	}

}
