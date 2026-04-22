package eec.epi.scripts;

import org.apache.commons.collections4.ListUtils;
import org.apache.lucene.util.SameThreadExecutorService;
import org.meveo.commons.utils.MethodCallingUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.IEntity;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.shared.DateUtils;
import org.meveo.security.MeveoUser;
import org.meveo.security.keycloak.CurrentUserProvider;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.crm.impl.ProviderService;
import org.meveo.service.custom.CustomTableService;
import org.meveo.service.job.JobExecutionService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.script.Script;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * common methods used by context-specific script classes
 */
@SuppressWarnings("serial")
public abstract class ACommonScript extends Script implements ServiceAble {

	MethodCallingUtils methodCallingUtils = getServiceInterface(
			MethodCallingUtils.class);

//	/**
//	 * execute a job after setting its params
//	 *
//	 * @param jobCode
//	 *          code of the job to execute
//	 * @param params
//	 *          params to transmit to the job, nullable. Note : sending a value
//	 *          null will result in empty string ( "" ) being sent instead.
//	 */
//	public void execJobWithParam(String jobCode, Map<String, Object> params) {
//		CurrentUserProvider currentUserProvider = (CurrentUserProvider) getServiceInterface(
//				CurrentUserProvider.class.getSimpleName());
//		JobExecutionService jobExecutionService = (JobExecutionService) getServiceInterface(
//				JobExecutionService.class.getSimpleName());
//		JobInstanceService jobInstanceService = (JobInstanceService) getServiceInterface(
//				JobInstanceService.class.getSimpleName());
//		ProviderService providerService = (ProviderService) getServiceInterface(ProviderService.class.getSimpleName());
//
//		MeveoUser currentUser = currentUserProvider.getCurrentUser(
//		);
//
//		JobInstance jobInstance = jobInstanceService.findByCode(jobCode);
//		if (params != null && !params.isEmpty()) {
//			jobInstance.setCfValue("ScriptingJob_variables", params);
//		}
//		jobExecutionService.executeJobAsync(jobInstance, null, null, currentUser);
//	}

	/**
	 * print a time in ms to human format.
	 * <ol>
	 * </li>up to 1min the millis are used</li></li>otherwise, hours minutes and
	 * seconds are used</li>
	 * </ol>
	 *
	 * @param millis
	 * @return a new string representing the millis
	 */
	protected String printTime(long millis) {
		if (millis < 1000) {
			return "" + millis + "ms";
		}
		long seconds = millis / 1000;
		millis = millis % 1000;

		// to 1min
		if (seconds < 60) {
			return "" + seconds + "s" + (millis == 0 ? "" : "" + millis + "ms");
		}

		long minutes = seconds / 60;
		seconds = seconds % 60;
		long hours = minutes / 60;
		minutes = minutes % 60;
		return (hours > 0 ? "" + hours + "h" : "") + (minutes > 0 ? "" + minutes + "min" : "")
				+ (seconds > 0 ? "" + seconds + "s" : "");
	}

	protected EntityManager em() {
		return getServiceInterface(EdrService.class).getEntityManager();
	}

	/**
	 * get the hibernate dialect used. Should be tested to contain "postgres" for
	 * postgres DB, "oracle" for oracle DB.
	 * <ul>
	 * <li>oracle : org.meveo.commons.persistence.oracle.MeveoOracleDialect</li>
	 * <li>postgres :
	 * org.meveo.commons.persistence.postgresql.MeveoPostgreSQLDialect</li>
	 * </ul>
	 */
	protected String hibernateDialect() {
		return em().getEntityManagerFactory().getProperties().get("hibernate.dialect").toString();
	}

	/**
	 * split a list in sublist to avoid inList maximum size in oracle.
	 *
	 * @param <T>
	 *          Type of the items in the list
	 * @param items
	 *          list of items to split
	 * @return a new List that contains the items if its size is small enough, or
	 *         the items split in sublists of small enough size otherwise.
	 */
	public static <T> List<List<T>> splitInList(List<T> items) {
		final int maxValue = ParamBean.getInstance().getPropertyAsInteger("database.number.of.inlist.limit",
				PersistenceService.SHORT_MAX_VALUE);
		if (items.size() > maxValue) {
			return ListUtils.partition(items, maxValue);
		} else {
			return List.of(items);
		}
	}

	/**
	 * execute a supplier in a new transaction that is subsequently saved. If the
	 * supplier produces an exception, then the transaction is rolled back and the
	 * result must be handled in the current transaction, consuming the supplied
	 * handling result if any.
	 * <p>
	 * Note : the reason why a transaction is not committed is irrelevant. The
	 * important part is that, if the execution throws an exception, if it is of
	 * type {@link RollbackRequestException} then the embeded result is used ;
	 * otherwise an {@link HandlingResult#error(String)} is used.
	 * </p>
	 *
	 * @param exec
	 *          supplier of the handling result. In order to trigger a rollback,
	 *          should throw an exception. In order to rollback with a success
	 *          result, should throw a {@link RollbackRequestException}. This
	 *          should NEVER return null.
	 * @param notSaved
	 *          handler when the execution was not saved, typically to save the
	 *          data in the current transaction.
	 */
	protected void execInTx(Supplier<HandlingResult> exec,
			Consumer<HandlingResult> notSaved) {
		// result transmitted by the execution. if error is caught, will be an
		// error result instead.
		HandlingResult result = null;
		// exception on execution. when not null, the item is considered as error
		Throwable ex = null;
		try {
			result = methodCallingUtils.callCallableInNewTx(() -> exec.get());
			if (result == null) {
				ex = new NullPointerException("exec returned null result");
				result = HandlingResult.error(ex.getMessage());
			}
		} catch (RollbackRequestException rre) {
			result = rre.result;
			ex = rre;
		} catch (Exception e2) {
			ex = e2;
		}
		if (notSaved != null && ex != null) {
			notSaved.accept(result);
		}
	}

	/**
	 * execute each item of an iterable in a separate transaction, then apply
	 * specific call for those that resulted in a rollback . This is required
	 * because the rollback in a transaction must be done using exception throw.
	 *
	 * @param <U>
	 *          the type of items we iterate over.
	 * @param items
	 *          iterable of items to handle. Note that since each item can be
	 *          handled in a different transaction, then items that can produce
	 *          mutually exclusive modifications, like comitting different values
	 *          for the same DB line, should be grouped, therefore using a List as
	 *          U instead..
	 * @param exec
	 *          method to apply to each item. Should never return null. If this
	 *          throws a {@link RollbackRequestException} then the embedded result
	 *          is used. Typically to rollback the result and still consider the
	 *          handling a success, use
	 *          {@link RollbackRequestException#handlingSuccess()} in that method.
	 * @param notSaved
	 *          method to handle the list items that produced a rollback in their
	 *          transaction. Typically save the data as-is. This call is made in
	 *          the main transaction
	 * @param parrallel
	 *          When set to true, the transactions can be spawned in parrallel.
	 *          Note that when parrallel is false, this is functionnally
	 *          <pre>items.foreach(
	 *            u-&gt;execInTx(()-&gt;exec.apply(u) ,
	 *             h-&gt;notSaved.accept(List.of(new SimpleEntry(u, h)))
	 *            )</pre> However with unnecessary complexity. This is basically
	 *          provided for easy debug.
	 */
	protected <U> void execInTx(Iterable<U> items, Function<U, HandlingResult> exec,
			Consumer<List<SimpleEntry<U, HandlingResult>>> notSaved, boolean parrallel) {
		ExecutorService executor;
		if (parrallel) {
			try {
				executor = (ExecutorService) new InitialContext()
						.lookup("java:jboss/ee/concurrency/executor/job_executor");
			} catch (NamingException e) {
				throw new UnsupportedOperationException("can't load executor", e);
			}
		} else {
			executor = new SameThreadExecutorService();
		}
		List<SimpleEntry<U, Future<HandlingResult>>> tasks = new ArrayList<>();
		for (U u : items) {
			Future<HandlingResult> future = executor.submit(() -> {
				return methodCallingUtils.callCallableInNewTx(() -> exec.apply(u));
			});
			tasks.add(new SimpleEntry<>(u, future));
		}
		List<SimpleEntry<U, HandlingResult>> errors = new ArrayList<>();
		for (SimpleEntry<U, Future<HandlingResult>> e : tasks) {
			U u = e.getKey();
			Future<HandlingResult> future = e.getValue();
			// result transmitted by the execution. if error is caught, will be an
			// error result instead.
			HandlingResult result = null;
			// exception on execution. when not null, the item is considered as error
			Throwable ex = null;
			try {
				result = future.get();
				if (result == null) {
					ex = new NullPointerException("exec returned null result for item " + u);
					result = HandlingResult.error(ex.getMessage());
				}
			} catch (ExecutionException ee) {
				Throwable cause = ee.getCause();
				if (cause instanceof RollbackRequestException) {
					RollbackRequestException rre = (RollbackRequestException) cause;
					result = rre.result;
					ex = cause;
				} else {
					result = HandlingResult.error(cause.getMessage());
					ex = cause;
				}
			} catch (Exception e2) {
				ex = e2;
			}
			if (ex != null) {
				errors.add(new SimpleEntry<>(u, result));
			}
		}
		if (notSaved != null && !errors.isEmpty()) {
			notSaved.accept(errors);
		}
	}

	protected static final String FIELD_ID = "id";

	//
	// start mass update
	//

	/**
	 * mass update CT entries
	 *
	 * @param tableName
	 * @param records
	 * @return
	 */
	public int massUpdate(String tableName, List<Map<String, Object>> records) {
		String query = null;

		if (records.size() > 1) {
			String dialect = hibernateDialect();
			if (dialect.contains("postgres")) {
				// TODO when the postgres query is done. Until then, can't mass update
				// query = makeMassUpdatePostgres(tableName, records);
			} else if (dialect.contains("oracle")) {
				query = makeMassUpdateOracle(tableName, records);
			}
			if (query == null) {
				log.warn("mass update in dialect " + dialect + " gives null query");
			}
		}

		if (query != null) {
			return em().createNativeQuery(query).executeUpdate();
		} else {
			getServiceInterface(CustomTableService.class).update(tableName, records);
			return records.size();
		}
	}

	String makeMassUpdatePostgres(String tableName, List<Map<String, Object>> records) {
		// TODO how to do that in postgres ?
		return null;
	}

	String makeMassUpdateOracle(String tableName, List<Map<String, Object>> records) {
		LinkedHashMap<String, LinkedHashSet<String>> fields = normalizeFields(records);
		StringBuilder builder = new StringBuilder("update(select * from\n  ");
		builder.append(tableName).append(" present_values\n  join(\n    select id");
		for (String fld : fields.keySet()) {
			if (FIELD_ID.compareToIgnoreCase(fld == null ? "" : fld) != 0) {
				builder.append(", max(").append(fld).append(") as updated_").append(fld);
			}
		}
		builder.append("\n    from(\n");
		// create the table as union all from the records
		boolean firstRow = true;
		for (Map<String, Object> record : records) {
			builder.append(firstRow ? "      " : "      union all ").append("select ");
			boolean firstCol = true;
			for (Entry<String, LinkedHashSet<String>> es : fields.entrySet()) {
				if (!firstCol) {
					builder.append(", ");
				}
				Object firstFound = es.getValue().stream().map(k -> record.get(k)).filter(o -> o != null).findFirst()
						.orElse(null);
				builder.append(exportOracleObject(firstFound));
				if (firstRow) {
					builder.append(" as ").append(es.getKey());
				}
				firstCol = false;
			}
			builder.append(" from dual\n");
			firstRow = false;
		}

		builder.append("    ) group by " + FIELD_ID + "\n  ) update_values on present_values." + FIELD_ID
				+ "=update_values." + FIELD_ID + ")\nset\n");
		// append the field updates
		boolean first = true;
		for (String fld : fields.keySet()) {
			if (FIELD_ID.compareToIgnoreCase(fld == null ? "" : fld) != 0) {
				builder.append(first ? "  " : "  , ");
				builder.append(fld).append("= updated_").append(fld).append("\n");
				first = false;
			}
		}
		return builder.toString();
	}

	String exportOracleObject(Object toExport) {
		if (toExport == null) {
			return "null";
		}
		if (toExport instanceof String) {
			return "'" + escapeOracle(toExport.toString()) + "'";
		}
		if (toExport instanceof Long || toExport instanceof Integer || toExport instanceof BigInteger) {
			return "" + ((Number) toExport).longValue();
		}
		if (toExport instanceof Double || toExport instanceof Float || toExport instanceof BigDecimal) {
			return "" + ((Number) toExport).doubleValue();
		}
		if (toExport instanceof Date) {
			return "to_date('" + DateUtils.formatDateWithPattern((Date) toExport, "yyyy-MM-dd HH:mm:ss")
			+ "', 'YYYY-MM-DD HH24:MI:SS')";
		}
		if (toExport instanceof Boolean) {
			return ((Boolean) toExport).booleanValue() ? "1" : "0";
		}
		if (toExport instanceof IEntity) {
			return ((IEntity) toExport).getId().toString();
		}
		throw new UnsupportedOperationException();
	}

	String escapeOracle(String source) {
		return source
				.replaceAll("'", "''")
				.replaceAll("&", "&'||'");
	}

	/**
	 * normalize the fields of a series of records.
	 *
	 * @param records
	 *          records from which to consider the fields.
	 * @return an ordered map from normalized field name to the list of existing
	 *         fields
	 */
	LinkedHashMap<String, LinkedHashSet<String>> normalizeFields(List<Map<String, Object>> records) {
		LinkedHashMap<String, LinkedHashSet<String>> ret = new LinkedHashMap<>();
		records.stream().flatMap(r -> r.keySet().stream()).map(String::toUpperCase).distinct().sorted()
		.forEach(name -> ret.computeIfAbsent(name.toUpperCase(), s -> new LinkedHashSet<>()));
		records.stream().flatMap(r -> r.keySet().stream()).distinct().sorted()
		.forEach(name -> ret.get(name.toUpperCase()).add(name));
		return ret;
	}

}
