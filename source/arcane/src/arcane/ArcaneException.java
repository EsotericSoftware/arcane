
package arcane;

public class ArcaneException extends RuntimeException {
	public ArcaneException (String message, Throwable cause) {
		super(message, cause);
	}

	public ArcaneException (String message) {
		super(message);
	}

	public ArcaneException () {
		super();
	}

	public ArcaneException (Throwable cause) {
		super(cause);
	}
}
