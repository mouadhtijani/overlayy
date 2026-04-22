package eec.epi.scripts;

import java.util.Objects;

public class CreationResult<T> extends HandlingResult {

	public final T result;

	protected CreationResult(ResultType type, String error, String code, T result) {
		super(type, error, code);
		this.result = result;
	}

	public static final CreationResult<?> SKIP = new CreationResult<>(ResultType.SKIP, null, null, null);

	@SuppressWarnings("unchecked")
	public static <U> CreationResult<U> skip() {
		return (CreationResult<U>) SKIP;
	}

	public static <U> CreationResult<U> ok(U result) {
		return new CreationResult<>(ResultType.SUCCESS, null, null, result);
	}

	public static <U> CreationResult<U> fail(String message, String code) {
		return new CreationResult<>(ResultType.ERROR, message, code, null);
	}

	public static <U> CreationResult<U> fail(String message) {
		return new CreationResult<>(ResultType.ERROR, message, null, null);
	}

	@Override
	public String toString() {
		return super.toString() + ":" + String.valueOf(result);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + (result == null ? 0 : result.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !obj.getClass().equals(this.getClass())) {
			return false;
		}
		CreationResult<?> other = (CreationResult<?>) obj;
		return Objects.equals(type, other.type)
				&& Objects.equals(error, other.error)
				&& Objects.equals(code, other.code)
				&& Objects.equals(result, other.result);
	}

}