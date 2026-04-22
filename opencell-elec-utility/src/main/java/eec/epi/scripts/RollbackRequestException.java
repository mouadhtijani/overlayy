package eec.epi.scripts;

import org.meveo.admin.exception.BusinessException;

/**
 * exception thrown when the current transaction should be rolled back, while
 * also embedding the actual result of the processing.
 */
@SuppressWarnings("serial")
public class RollbackRequestException extends BusinessException {

	public final HandlingResult result;

	public RollbackRequestException(HandlingResult result) {
		this.result = result;
	}

	public static void handlingFail(String message, String code) {
		throw new RollbackRequestException(HandlingResult.error(message, code));
	}

	public static void handlingFail(String message) {
		throw new RollbackRequestException(HandlingResult.error(message));
	}

	public static void handlingSuccess() {
		throw new RollbackRequestException(HandlingResult.SUCCESS);
	}

	public static void handlingSkip() {
		throw new RollbackRequestException(HandlingResult.SKIP);
	}

}
