package eec.epi.scripts;

import org.meveo.commons.utils.EjbUtils;
import org.slf4j.LoggerFactory;

public interface ServiceAble {

	/**
	 * try to get a service for given type in the current transaction context
	 *
	 * @param <U>
	 *          class we want
	 * @param clazz
	 *          the class of U
	 * @return existing service for given class, or null
	 */

	@SuppressWarnings("unchecked")
	default <U> U getServiceInterface(Class<U> clazz) {
		Object ret = EjbUtils.getServiceInterface(clazz.getSimpleName());
		if (ret == null || !clazz.isAssignableFrom(ret.getClass())) {
			LoggerFactory.getLogger(getClass()).error("found invalid object " + ret + " for class " + clazz);
			return null;
		}
		return (U) ret;
	}

}
