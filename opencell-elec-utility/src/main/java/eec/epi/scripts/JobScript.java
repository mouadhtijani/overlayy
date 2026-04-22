package eec.epi.scripts;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.response.PagingAndFiltering.SortOrder;
import org.meveo.commons.utils.ParamBean;
import org.meveo.service.crm.impl.ProviderService;
import org.meveo.service.custom.CustomTableService;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * common methods for a script that is executed as a job script.
 *
 * Most methods that interact with the job (through the {@link JobContext}) are
 * synchronized on the internal method context.
 *
 */
@SuppressWarnings("serial")
public abstract class JobScript extends ACommonScript {
	CustomTableService customTableService = getServiceInterface(CustomTableService.class.getSimpleName());

	public abstract void execute(JobContext jobContext);

	public class JobContext {

		private final Map<String, Object> methodContext;

		private final List<String> reports = new ArrayList<>();

		public JobContext(Map<String, Object> methodcontext) {
			methodContext = methodcontext;
		}

		/**
		 *
		 * @return the NOT SYNCHRONIZED method context.
		 */
		public Map<String, Object> getMethodContext() {
			return methodContext;
		}

		//

		/**
		 * get an option transmitted, or a default one if absent
		 *
		 * @param name
		 *          case-sensitive option name, as referenced in the job.
		 * @param defaultValue
		 *          nullable default value returned when the option is not set.
		 */
		@SuppressWarnings("unchecked")
		public <U> U getOption(String name, U defaultValue) {
			synchronized (methodContext) {
				return (U) methodContext.getOrDefault(name, defaultValue);
			}
		}

		//
		// reporting tools
		//

		/**
		 * add a report as the last one.
		 *
		 * @param messageToAppend
		 *          report message
		 */
		public void addReport(String messageToAppend) {
			synchronized (methodContext) {
				reports.add(messageToAppend);
			}
		}

		/**
		 * add a report as the first one.
		 *
		 * @param messageToAppend
		 *          report message
		 */
		public void addReportFront(String messageToAppend) {
			synchronized (methodContext) {
				reports.add(0, messageToAppend);
			}

		}

		////

		/** add an OK to the result */
		public void addOK() {
			addOK(1);
		}

		/** add several OK to the result */
		public void addOK(int number) {
			synchronized (methodContext) {
				Long oks = (Long) methodContext.get(JOB_RESULT_NB_OK);
				oks = (oks == null ? 0l : oks) + number;
				methodContext.put(JOB_RESULT_NB_OK, oks);
			}
		}

		/**
		 * report an ok. The OK count is increased and the message is stored
		 */
		public void reportOK(String message) {
			reportOK(message, 1);
		}

		/**
		 * report n ok. The OK count is increased and the message is stored
		 *
		 * @param number
		 *          the number of ok to add (default 1)
		 */
		public void reportOK(String message, int number) {
			addOK(number);
			if (number > 1) {
				message = "" + number + " × " + message;
			}
			addReport(formatOK(message));
			log.debug(message);
		}

		public long nbOK() {
			return (long) methodContext.getOrDefault(JOB_RESULT_NB_OK, 0l);
		}

		////

		public void addWARN() {
			addWARN(1);
		}

		public void addWARN(int number) {
			synchronized (methodContext) {
				Long warns = (Long) methodContext.get(JOB_RESULT_NB_WARN);
				warns = (warns == null ? 0l : warns) + number;
				methodContext.put(JOB_RESULT_NB_WARN, warns);
			}
		}

		public void reportWARN(String message) {
			reportWARN(message, 1);
		}

		/**
		 * report n warnings. The WARN count is increased and the message is stored
		 *
		 * @param number
		 *          the number of warnings to add (default 1)
		 */
		public void reportWARN(String message, int number) {
			addWARN(number);
			if (number > 1) {
				message = "" + number + " × " + message;
			}
			addReport(formatWARN(message));
			log.warn(message);
		}

		public long nbWarn() {
			return (long) methodContext.getOrDefault(JOB_RESULT_NB_WARN, 0l);
		}

		////

		public void addKO() {
			addKO(1);
		}

		public void addKO(int number) {
			synchronized (methodContext) {
				Long kos = (Long) methodContext.get(JOB_RESULT_NB_KO);
				kos = (kos == null ? 0l : kos) + number;
				methodContext.put(JOB_RESULT_NB_KO, kos);
			}
		}

		public void reportKO(String message) {
			reportKO(message, 1);
		}

		public void reportKO(String message, int number) {
			addKO(number);
			if (number > 1) {
				message = "" + number + " × " + message;
			}
			addReport(formatKO(message));
			log.error(message);
		}

		public void reportInfo(String message) {
			reportInfo(message, 1);
		}

		public void reportInfo(String message, int number) {
			if (number > 1) {
				message = "" + number + " × " + message;
			}
			addReport(formatInfo(message));
			log.info(message);
		}

		public long nbKO() {
			return (long) methodContext.getOrDefault(JOB_RESULT_NB_KO, 0l);
		}

		///

		public void addToProcess(long number) {
			synchronized (methodContext) {
				long toProcess = (long) methodContext.getOrDefault(JOB_RESULT_TO_PROCESS, 0l);
				methodContext.put(JOB_RESULT_TO_PROCESS, number + toProcess);
			}
		}

		public long nbToProcess() {
			return (long) methodContext.getOrDefault(JOB_RESULT_TO_PROCESS, 0l);
		}

		//

		/**
		 * store the list of reports, inside the actual method context
		 */
		protected void storeReports() {
			synchronized (methodContext) {
				methodContext.put(JOB_RESULT_REPORT, reports.stream().collect(Collectors.joining("\n")));
			}
		}

	}

	public String formatKO(String message) {
		return " FAIL : " + message;
	}

	public String formatOK(String message) {
		return "OK : " + message;
	}

	public String formatWARN(String message) {
		return "WARN : " + message;
	}

	public String formatInfo(String message) {
		return "   INFO : " + message;
	}

	@Override
	public void execute(Map<String, Object> methodContext) throws BusinessException {
		prepareResults(methodContext);
		JobContext jobContext = new JobContext(methodContext);
		execute(jobContext);
		postExec(jobContext);
	}

	protected void prepareResults(Map<String, Object> methodContext) {
		methodContext.put(JOB_RESULT_NB_OK, 0l);
		methodContext.put(JOB_RESULT_NB_WARN, 0l);
		methodContext.put(JOB_RESULT_NB_KO, 0l);
	}

	protected void postExec(JobContext jobContext) {
		jobContext.storeReports();
		log.debug("script result : {} OK, {} WARN, {} KO", jobContext.getMethodContext().getOrDefault(JOB_RESULT_NB_OK, 0l),
				jobContext.getMethodContext().getOrDefault(JOB_RESULT_NB_WARN, 0l),
				jobContext.getMethodContext().getOrDefault(JOB_RESULT_NB_KO, 0l));
	}

	//
	// required files tools
	//

	/**
	 * ensure the dir is present from root path of application
	 *
	 * @param pathFromRoot
	 * @param context
	 *          context to add a KO on error.
	 * @return such a File, if exist or is created ; null if can't exist. Also
	 *         adds a KO in the context if can't exist.
	 */
	public File requireDir(String pathFromRoot, JobContext context) {
		String rootPath = ParamBean.getInstance()
				.getChrootDir(getServiceInterface(ProviderService.class).getProvider().getCode());
		return requireDir(new File(rootPath), pathFromRoot, context);
	}

	/**
	 *
	 * ensure the dir is present as a child from its parent
	 *
	 * @param parentFile
	 *          parent file, typical already checked
	 * @param childPath
	 *          path from the parent. can be depth >1 eg a/b/c
	 * @param context
	 *          context to add a KO on error.
	 * @return
	 */
	public File requireDir(File parentFile, String childPath, JobContext context) {
		File ret = new File(parentFile, childPath);
		if (!ret.exists()) {
			if (!ret.mkdirs()) {
				context.reportKO("can't create dir " + ret.getAbsolutePath());
				return null;
			}
		}
		if (!ret.isDirectory()) {
			context.reportKO("file already exists and is not a dir " + ret.getAbsolutePath());
			return null;
		}
		return ret;
	}

	//
	// query tools
	//

	/**
	 * execute an update query and notifies the job context of the resulting OK
	 *
	 * @param jobContext
	 *          the context of the job in which we execute this script
	 * @param query
	 *          the updating query
	 * @param message
	 *          optional message to present in the job context. use empty message
	 *          to only show the duration of the request
	 */
	protected void updateOK(JobContext jobContext, String query, String message) {
		long start = System.currentTimeMillis();
		int updated = em().createNativeQuery(query).executeUpdate();
		long stop = System.currentTimeMillis();
		if (updated > 0) {
			if (message != null) {
				jobContext.reportOK(message + " (in " + printTime(stop - start) + ")", updated);
			} else {
				jobContext.addOK(updated);
			}
		}
	}

	/**
	 * execute an update query and notifies the job context of the resulting KO.
	 * Typically this updates the DB so the error data won't be processed into the
	 * standard updating queries.
	 *
	 * @param jobContext
	 *          the context of the job in which we execute this script
	 * @param query
	 *          the updating query
	 * @param message
	 *          optional message to present in the job context. use empty message
	 *          to only show the duration of the request
	 */
	protected void updateError(JobContext jobContext, String query, String message) {
		long start = System.currentTimeMillis();
		int updated = em().createNativeQuery(query).executeUpdate();
		long stop = System.currentTimeMillis();
		if (updated > 0) {
			if (message != null) {
				jobContext.reportKO(message + " (in " + printTime(stop - start) + ")", updated);
			} else {
				jobContext.addKO(updated);
			}
		}
	}

	/**
	 * execute an update query and notifies the job context of the resulting INFO.
	 * Typically this updates the DB so the error data won't be processed into the
	 * standard updating queries.
	 *
	 * @param jobContext
	 *          the context of the job in which we execute this script
	 * @param query
	 *          the updating query
	 * @param message
	 *          optional message to present in the job context. use empty message
	 *          to only show the duration of the request
	 */
	protected void updateInfo(JobContext jobContext, String query, String message) {
		long start = System.currentTimeMillis();
		int updated = em().createNativeQuery(query).executeUpdate();
		long stop = System.currentTimeMillis();
		if (updated > 0) {
			if (message != null) {
				jobContext.reportInfo(message + " (in " + printTime(stop - start) + ")", updated);
			}
		}
	}

	/**
	 * execute an update query without notifying the job context of the result.
	 *
	 * @param query
	 *          the updating query
     */
	protected void updateSilent(String query) {
		em().createNativeQuery(query).executeUpdate();
	}

	/**
	 * for each line returned by the query, report a KO containing the error
	 * message then the line.
	 *
	 */
	protected int selectErrors(JobContext jobContext, String query, String errorMessage) {
		@SuppressWarnings("unchecked")
		List<? extends Object> result = em().createNativeQuery(query).getResultList();
		int count = 0;
		for (Object o : result) {
			if (o.getClass().isArray()) {
				jobContext.reportKO(errorMessage + List.of((Object[]) o));
			} else {
				jobContext.reportKO(errorMessage + o);
			}
			count++;
		}
		return count;
	}

	/**
	 * report errored lines, or an OK if no error.
	 */
	protected void selectErrors(JobContext jobContext, String query, String errorMessage, String OKMessage) {
		int count = selectErrors(jobContext, query, errorMessage);
		if (count == 0) {
			jobContext.reportOK(OKMessage);
		}
	}

	//
	// batch tools
	//

	/**
	 * iterate over the entries in a custom table, each in a separate transaction.
	 * <p>
	 * Entries are fetch by batch, to reduce memory footprint. They fetched by id
	 * increasing.
	 * </p>
	 * <p>
	 * When parallel, and the grouper is specified, the lines fetched that have
	 * the same group are executed in sequential transactions. Basically the first
	 * elements of each group are handled in parallel, then the second elements,
	 * etc. . If the grouper is null or produces null for a line, then
	 * corresponding line are each in their own group and may be handled in
	 * parallel transaction.
	 * </p>
	 * <p>
	 * For each entry processed, the result of the processing is automatically
	 * added to the job report. Errors are added each as a report, while successes
	 * are only counted. Also, a final message is added to the job report
	 * specifying the time elapsed, nb of OK, SKIP, FAIL received.
	 * </p>
	 * <p>
	 * When the handler produces an exception, the transaction is rolled back and
	 * the entry along with the result will be processed in the main transaction,
	 * typically to save its status as error, using notSaved
	 * </p>
	 *
	 * @param jobContext
	 *          the context of the job that started the processing.
	 * @param tableName
	 *          CT name
	 * @param filters
	 *          additional {@link PaginationConfiguration#getFilters()} filters
	 * @param grouper
	 *          optional function to group together lines that should NOT be
	 *          executed in parrallel.
	 * @param fetchSize
	 *          max number of items to retrieve per fetch
	 * @param parrallel
	 *          when set to true, transactions are spawned in different threads
	 * @param handler
	 *          function to apply on each entry. Should never return null.
	 * @param notSaved
	 *          function to apply when the handler rolled back the transaction,
	 *          for example after an exception.
	 */
	public void execCTBatchInTx(JobContext jobContext,
			String tableName, Map<String, Object> filters,
			Function<Map<String, Object>, Object> grouper,
			int fetchSize, boolean parrallel,
			BiFunction<JobContext, Map<String, Object>, HandlingResult> handler,
			Consumer<List<SimpleEntry<Map<String, Object>, HandlingResult>>> notSaved) {
		Objects.requireNonNull(jobContext);
		Objects.requireNonNull(tableName);
		if (filters == null) {
			filters = Collections.emptyMap();
		}
		Objects.requireNonNull(handler);

		Long lastRetrieved = null;
		boolean ctn = true;
		long start = System.currentTimeMillis();
		int done = 0;
		int[] countSuccessErrorSkip = { 0, 0, 0 };
		CustomTableService customTableService = getServiceInterface(CustomTableService.class);
		while (ctn) {
			Map<String, Object> filters2 = new HashMap<>(filters);
			if (lastRetrieved != null) {
				filters2.put("fromRangeExclusive " + FIELD_ID, lastRetrieved);
			}
			PaginationConfiguration pc = new PaginationConfiguration(null, fetchSize < 1 ? null : fetchSize, filters2, null,
					null, FIELD_ID, SortOrder.ASCENDING);
			List<Map<String, Object>> fetched = customTableService.list(tableName, pc);

			if (!fetched.isEmpty()) {
				lastRetrieved = fetched.stream().mapToLong(m -> ((Number) m.getOrDefault("id", -1)).longValue()).max()
						.orElse(-1);
				if (lastRetrieved < 0) {
					ctn = false;
				}
				long batchStart = System.currentTimeMillis();

				Map<Object, List<Map<String, Object>>> entriesByGroup = new LinkedHashMap<>();
				List<Map<String, Object>> entriesWOGroup = new ArrayList<>();
				for (Map<String, Object> l : fetched) {
					if (grouper == null) {
						entriesWOGroup.add(l);
					} else {
						try {
							Object group = grouper.apply(l);
							if (group != null) {
								entriesByGroup.computeIfAbsent(group, o -> new ArrayList<>()).add(l);
							} else {
								entriesWOGroup.add(l);
							}
						} catch (Exception e) {
							log.error("when grouping " + l, e);
							HandlingResult h = HandlingResult.error(e.getMessage());
							if (notSaved != null) {
								notSaved.accept(List.of(new SimpleEntry<>(l, h)));
							}
							synchronized (countSuccessErrorSkip) {
								countSuccessErrorSkip[1]++;
							}
							jobContext.reportKO(e.getMessage());
						}
					}
				}

				/**
				 * at position i, the list of all i-th element of a group. non grouped
				 * are at pos 0
				 */
				List<List<Map<String, Object>>> entriesSequenced = new ArrayList<>();
				entriesSequenced.add(entriesWOGroup);
				for (List<Map<String, Object>> l : entriesByGroup.values()) {
					for (int i = 0; i < l.size(); i++) {
						if (i < entriesSequenced.size()) {
							entriesSequenced.get(i).add(l.get(i));
						} else {
							List<Map<String, Object>> seq_i = new ArrayList<>();
							seq_i.add(l.get(i));
							entriesSequenced.add(seq_i);
						}
					}
				}
				Consumer<HandlingResult> stats = result -> {
					switch (result.type) {
					case SKIP:
						synchronized (countSuccessErrorSkip) {
							countSuccessErrorSkip[2]++;
						}
						jobContext.addWARN();
						break;
					case SUCCESS:
						synchronized (countSuccessErrorSkip) {
							countSuccessErrorSkip[0]++;
						}
						jobContext.addOK();
						break;
					case ERROR:
						synchronized (countSuccessErrorSkip) {
							countSuccessErrorSkip[1]++;
						}
						jobContext.reportKO(result.error);
						break;
					default:
						throw new UnsupportedOperationException("case " + result.type + " not handled");
					}
				};
				for (List<Map<String, Object>> elems : entriesSequenced) {
					execInTx(elems, line -> {
						HandlingResult result = handler.apply(jobContext, line);
						stats.accept(result);
						return result;
					}, l -> {
						for (SimpleEntry<Map<String, Object>, HandlingResult> h : l) {
							stats.accept(h.getValue());
						}
						notSaved.accept(l);
					}, parrallel);
				}
				long time = System.currentTimeMillis();
				long timeElapsed = time - start;
				long batchTime = time - batchStart;
				done += fetched.size();
				String message = new StringBuilder("handled batch of ").append(fetched.size()).append(" items"
						+ " in ").append(printTime(batchTime)).append(" (").append(fetched.size() * 1000 / batchTime).append("i/s)"
								+ ", total ")
						.append(done).append(" in ").append(printTime(timeElapsed))
						.append(" (").append(done * 1000 / timeElapsed).append("gi/s)")
						.toString();
				log.debug(message);
			} else {
				ctn = false;
			}
		}
		long time = System.currentTimeMillis();
		long timeElapsed = time - start;
		String message = "handled " + done + " items in " + printTime(timeElapsed) + " with " + countSuccessErrorSkip[0]
				+ " success, " + countSuccessErrorSkip[2] + " skips, " + countSuccessErrorSkip[1] + " errors";
		jobContext.addReportFront(message);
		log.info(message);
	}
	/**
	 * get Data from Custom Table
	 * @return Custom table List
	 */
	protected List<Map<String, Object>> getFromCT(String table, Map<String, Object> filter) {
		try {
			return customTableService.list(table, new PaginationConfiguration(filter));
		} catch (Exception e) {
			return null;
		}
	}

}
