package eec.epi.scripts;

import java.util.Objects;

/**
 * result of the handling of a data. Should be either the {@link #SUCCESS}, a
 * {@link #SKIP}, or an {@link #error} with a message and an optional code
 *
 */
public class HandlingResult {

	public enum ResultType {
		SKIP, SUCCESS, ERROR
	}

	public static final HandlingResult SUCCESS = new HandlingResult(ResultType.SUCCESS, null, null);
	public static final HandlingResult SKIP = new HandlingResult(ResultType.SKIP, null, null);
	public final ResultType type;
	public final String error;
	public final String code;

	protected HandlingResult(ResultType type, String error, String code) {
		this.type = type;
		this.error = error;
		this.code = code;
	}

	public static HandlingResult error(String message, String code) {
		return new HandlingResult(ResultType.ERROR, message, code);
	}

	public static HandlingResult error(String message) {
		return new HandlingResult(ResultType.ERROR, message, null);
	}

	public boolean isError() {
		return type == ResultType.ERROR;
	}

	public boolean isSuccess() {
		return type == ResultType.SUCCESS;
	}

	public boolean isSkip() {
		return type == ResultType.SKIP;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(type.toString());
		if (error != null) {
			sb.append(";error=" + error);
		}
		if (code != null) {
			sb.append(";code=" + code);
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return type.hashCode() + (error == null ? 0 : error.hashCode()) + (code == null ? 0 : code.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		HandlingResult other = (HandlingResult) obj;
		return Objects.equals(type, other.type)
				&& Objects.equals(error, other.error)
				&& Objects.equals(code, other.code);
	}

}
