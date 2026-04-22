//package eec.epi.beans.utils;
//
//
//import eec.epi.scripts.HandlingResult;
//import eec.epi.scripts.JobScript;
//import eec.epi.scripts.JobScript.JobContext;
//import eec.epi.scripts.WSScript.WSContext;
//
//import org.hibernate.ScrollMode;
//import org.hibernate.ScrollableResults;
//import org.hibernate.Session;
//import org.hibernate.query.NativeQuery;
//import org.meveo.admin.util.pagination.PaginationConfiguration;
//import org.meveo.api.dto.response.PagingAndFiltering.SortOrder;
//import org.meveo.commons.utils.ParamBean;
//import org.meveo.commons.utils.QueryBuilder;
//import org.meveo.model.IEntity;
//import org.meveo.model.transformer.AliasToEntityOrderedMapResultTransformer;
//import org.meveo.service.base.BaseService;
//import org.meveo.service.base.PersistenceService;
//import org.meveo.service.crm.impl.ProviderService;
//import org.meveo.service.custom.CustomTableService;
//
//import javax.ejb.Stateless;
//import javax.ejb.TransactionManagement;
//import javax.ejb.TransactionManagementType;
//import javax.inject.Inject;
//import javax.persistence.EntityManager;
//import javax.persistence.PersistenceContext;
//import javax.transaction.HeuristicMixedException;
//import javax.transaction.HeuristicRollbackException;
//import javax.transaction.NotSupportedException;
//import javax.transaction.RollbackException;
//import javax.transaction.Status;
//import javax.transaction.SystemException;
//import javax.transaction.UserTransaction;
//import java.io.File;
//import java.math.BigDecimal;
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.BiConsumer;
//import java.util.function.BiFunction;
//import java.util.function.Consumer;
//import java.util.function.Function;
//import java.util.function.Predicate;
//
//@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
//public class TransactionManagingService extends BaseService {
//
//	@PersistenceContext
//	protected EntityManager entityManager;
//
//	@Inject
//	protected CustomTableService customTableService;
//
//	@Inject
//	protected UserTransaction userTransaction;
//	@Inject
//	protected ProviderService providerService;
//
//	/**
//	 * Execute a call inside a transaction
//	 *
//	 * @param exec
//	 *          call to execute
//	 * @param onError
//	 *          handler if the program or the transaction management returns an
//	 *          exception
//	 */
//	public void exec(Runnable exec, Consumer<Exception> onError) {
//		try {
//			userTransaction.begin();
//			exec.run();
//			userTransaction.commit();
//		} catch (Exception e) {
//			if (onError != null) {
//				onError.accept(e);
//			}
//			log.error("caught in transaction", e);
//		} finally {
//			try {
//				if (userTransaction.getStatus() == Status.STATUS_ACTIVE
//						|| userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
//					userTransaction.rollback();
//				}
//			} catch (Throwable e) {
//				log.error("caught in rollback", e);
//				// ignore
//			}
//		}
//	}
//
//	/**
//	 * Execute a call inside a transaction
//	 *
//	 * @param exec
//	 *          call to execute
//	 */
//	public void exec(Runnable exec) {
//		exec(exec, null);
//	}
//
//	/**
//	 * loop over items , handle each of them, and save the transaction. Must be
//	 * called inside a transaction.
//	 *
//	 * @param jobContext
//	 *          context of the job who called this
//	 * @param listItems
//	 *          items to handle
//	 * @param handler
//	 *          handler of items
//	 * @param saveEvery
//	 *          number of time we must save the items
//	 * @param <T>
//	 *          type of the items we handle
//	 */
//	protected <T> void loopItems(JobScript.JobContext jobContext, Collection<T> listItems,
//                                 BiFunction<JobScript.JobContext, T, HandlingResult> handler, int saveEvery) {
//		jobContext.addToProcess(listItems.size());
//		long start = System.currentTimeMillis();
//		long lastCheckPoint = start;
//		int done = 0;
//		int skipCount = 0, successCount = 0, errorCount = 0;
//		for (T item : listItems) {
//			HandlingResult result;
//			try {
//				result = handler.apply(jobContext, item);
//			} catch (Exception e) {
//				log.error("while handlingitem " + item, e);
//				Throwable cause = e;
//				while (cause.getCause() != null) {
//					cause = cause.getCause();
//				}
//				result = HandlingResult.error("for item " + item + " : " + cause.getMessage());
//			}
//			switch (result.type) {
//			case SKIP:
//				skipCount++;
//				break;
//			case SUCCESS:
//				successCount++;
//				jobContext.addOK();
//				break;
//			case ERROR:
//				errorCount++;
//				jobContext.reportKO(result.error);
//				break;
//			default:
//				throw new UnsupportedOperationException("case " + result.type + " not handled");
//			}
//			done++;
//			if (done % saveEvery == 0) {
//				long time = System.currentTimeMillis();
//				log.info("saving transaction after processing " + done + "/" + listItems.size() + " entries, processed last "
//						+ saveEvery + " in " + printTime(time - lastCheckPoint));
//				lastCheckPoint = time;
//				if (!savePoint(jobContext)) {
//					return;
//				}
//			}
//		}
//		long timeElapsed = System.currentTimeMillis() - start;
//		String message = "handled " + listItems.size() + " items in " + printTime(timeElapsed) + " with " + successCount
//				+ " success, " + skipCount + " skips, " + errorCount + " errors";
//		jobContext.addReportFront(message);
//		log.info(message);
//	}
//
//	/**
//	 *
//	 * @return max delay after which we commit regardless of window size
//	 */
//	public float maxDelayCommitS() {
//		return 2;
//	}
//
//	public <T> void execBatchOnPaged(JobContext jobContext, BiConsumer<Map<String, Object>, Long> limitAdder, String sortField,
//			Function<PaginationConfiguration, List<T>> fetcher, Map<String, Object> filters,
//			Function<List<T>, Long> lastRetriever, BiConsumer<JobScript.JobContext, List<T>> handler, int fetchSize,
//			Predicate<JobContext> init) {
//		Objects.requireNonNull(jobContext);
//		Objects.requireNonNull(limitAdder);
//		Objects.requireNonNull(sortField);
//		Objects.requireNonNull(fetcher);
//		if (filters == null) {
//			filters = Collections.emptyMap();
//		}
//		Objects.requireNonNull(lastRetriever);
//		Objects.requireNonNull(handler);
//
//		Long lastRetrieved = null;
//		boolean ctn = true;
//		long start = System.currentTimeMillis();
//		int done = 0;
//		try {
//			userTransaction.begin();
//			if (init != null) {
//				boolean resInit = init.test(jobContext);
//				if (!resInit) {
//					jobContext.reportKO("init failed");
//					return;
//				}
//			}
//			while (ctn) {
//				Map<String, Object> filters2 = new HashMap<>(filters);
//				if (lastRetrieved != null) {
//					limitAdder.accept(filters2, lastRetrieved);
//				}
//				PaginationConfiguration pc = new PaginationConfiguration(null, fetchSize < 1 ? null : fetchSize, filters2, null,
//						null, sortField, SortOrder.ASCENDING);
//				// create an arraylist as copy because we need to be sure we can modify it
//				List<T> elems = new ArrayList<>(fetcher.apply(pc));
//				if (!elems.isEmpty()) {
//					lastRetrieved = lastRetriever.apply(elems);
//					if (lastRetrieved < 0) {
//						ctn=false;
//					}
//					long batchStart = System.currentTimeMillis();
//					handler.accept(jobContext, elems);
//					long time = System.currentTimeMillis();
//					long timeElapsed = time - start;
//					long batchTime = time - batchStart;
//					done+=elems.size();
//					String message = "handled batch of " + elems.size()
//					+ " items in " + printTime(batchTime)+" ("+elems.size()*1000/batchTime
//					+"i/s), total "+done+" in "+ printTime(timeElapsed)+" ("+done*1000/timeElapsed+"gi/s)" ;
//					log.debug(message);
//					savePoint(jobContext);
//				} else {
//					ctn = false;
//				}
//			}
//			long time = System.currentTimeMillis();
//			long timeElapsed = time - start;
//			String message = "handled " + done + " items in " + printTime(timeElapsed) ;
//			jobContext.addReportFront(message);
//			log.info(message);
//			userTransaction.commit();
//			log.info("last commit in " + printTime(System.currentTimeMillis() - time));
//		} catch (Exception e) {
//			log.error("caught in transaction", e);
//			jobContext.reportKO(e.getMessage());
//		} finally {
//			try {
//				if (userTransaction.getStatus() == Status.STATUS_ACTIVE
//						|| userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
//					userTransaction.rollback();
//				}
//			} catch (Throwable e) {
//				//should not happen
//				log.error("caught in rollback", e);
//				throw new UnsupportedOperationException(e);
//			}
//		}
//	}
//
//	public void  execBatchOnCT(JobContext jobContext, String tableName, Map<String, Object> filters,
//			BiConsumer<JobScript.JobContext, List<Map<String, Object>>> handler, int fetchSize, Predicate<JobContext> init) {
//		String idField="id";
//		execBatchOnPaged(
//				jobContext,
//				(m, l) -> {
//					if (l != null) {
//						m.put("fromRangeExclusive "+idField , l);
//					}
//				},
//				idField,
//				pc -> customTableService.list(tableName, pc),
//				filters,
//				l -> lastLine(l, idField),
//				handler,
//				fetchSize,
//				init
//				);
//	}
//
//	public void execBatchOnCT(JobContext jobContext, String tableName, Map<String, Object> filters,
//			BiConsumer<JobScript.JobContext, List<Map<String, Object>>> handler, int fetchSize){
//		execBatchOnCT(jobContext, tableName, filters, handler, fetchSize, null);
//	}
//
//	/**
//	 * list the entries of a CT into a handler. saves after each fetch.
//	 *
//	 * @param jobContext
//	 *          context to write messages into
//	 * @param limitAdder put the limit (2nd argument) in the filters (first argument)
//	 * @param sortField
//	 * @param fetcher
//	 * @param filters
//	 *          filters to apply when fetching the fetch
//	 * @param lastRetriever
//	 *          function to fetch the last id of the lines . should return -1 when
//	 *          a structural exception is caught (typically, the field does not
//	 *          exist)
//	 * @param handler handles an item in a context, and returns the processing result
//	 * @param fetchSize
//	 *          size of the list to retrieve each access.
//	 * @param init
//	 *          optional initializer ran inside the transaction. Typically fetches
//	 *          data in the DB. Should return false in case of error, in which
//	 *          case the job is stopped.
//	 */
//	public <T> void execOnPaged(JobContext jobContext, BiConsumer<Map<String, Object>, Long> limitAdder, String sortField,
//			Function<PaginationConfiguration, List<T>> fetcher, Map<String, Object> filters,
//			Function<List<T>, Long> lastRetriever, BiFunction<JobScript.JobContext, T, HandlingResult> handler, int fetchSize,
//			Predicate<JobContext> init) {
//		Objects.requireNonNull(jobContext);
//		Objects.requireNonNull(limitAdder);
//		Objects.requireNonNull(sortField);
//		Objects.requireNonNull(fetcher);
//		if (filters == null) {
//			filters = Collections.emptyMap();
//		}
//		Objects.requireNonNull(lastRetriever);
//		Objects.requireNonNull(handler);
//
//		long maxDelayCommitMS = (long) (1000 * maxDelayCommitS());
//		Long lastRetrieved = null;
//		boolean ctn = true;
//		long start = System.currentTimeMillis();
//		long lastLog = start;
//		int done = 0;
//		int skipCount = 0, successCount = 0, errorCount = 0;
//		try {
//			userTransaction.begin();
//			if (init != null) {
//				boolean resInit = init.test(jobContext);
//				if (!resInit) {
//					jobContext.reportKO("init failed");
//					return;
//				}
//			}
//			while (ctn) {
//				Map<String, Object> filters2 = new HashMap<>(filters);
//				if (lastRetrieved != null) {
//					limitAdder.accept(filters2, lastRetrieved);
//				}
//				PaginationConfiguration pc = new PaginationConfiguration(null, fetchSize < 1 ? null : fetchSize, filters2, null,
//						null, sortField, SortOrder.ASCENDING);
//				// create an arraylist as copy because we need to be sure we can modify it
//				List<T> elems = new ArrayList<>(fetcher.apply(pc));
//				if (!elems.isEmpty()) {
//					lastRetrieved = lastRetriever.apply(elems);
//					if (lastRetrieved < 0) {
//						ctn=false;
//					}
//
//					Map<T, HandlingResult> erroredElems = new HashMap<>();
//					Map<T, HandlingResult> passedElems = new HashMap<>();
//					boolean rerun = true;
//					// rerun is set to true whenever a processing throws an exception that
//					// invalidates the transaction.
//					// when this happens, we need to process the whole list again, except
//					// the items that have already created an exception.
//					while (rerun) {
//						rerun = false;
//						passedElems = new HashMap<>();
//						long nextCommitTime = System.currentTimeMillis() + maxDelayCommitMS;
//						for (T item : elems) {
//							if (erroredElems.containsKey(item)) {
//								continue;
//							}
//							try {
//								HandlingResult result = handler.apply(jobContext, item);
//								passedElems.put(item, result);
//							} catch (Exception e) {
//								Throwable cause = e;
//								while (cause.getCause() != null) {
//									cause = cause.getCause();
//								}
//								HandlingResult errored = HandlingResult
//										.error("while handling item " + item + " cause " + cause.getMessage());
//								log.error(errored.error, e);
//								if (userTransaction.getStatus() != Status.STATUS_ACTIVE) {
//									erroredElems.put(item, errored);
//									resetPoint(jobContext);
//									elems.removeAll(passedElems.keySet());
//									elems.addAll(passedElems.keySet());
//									rerun = true;
//									break;
//								} else {
//									passedElems.put(item, errored);
//								}
//							}
//							if (System.currentTimeMillis() > nextCommitTime) {
//								savePoint(jobContext);
//							}
//						}
//					}
//					List<HandlingResult> results = new ArrayList<>(passedElems.values());
//					results.addAll(erroredElems.values());
//					for (HandlingResult result : results) {
//						switch (result.type) {
//						case SKIP:
//							skipCount++;
//							jobContext.addWARN();
//							break;
//						case SUCCESS:
//							successCount++;
//							jobContext.addOK();
//							break;
//						case ERROR:
//							errorCount++;
//							jobContext.reportKO(result.error);
//							break;
//						default:
//							throw new UnsupportedOperationException("case " + result.type + " not handled");
//						}
//					}
//					done += passedElems.size();
//					long time = System.currentTimeMillis();
//					StringBuilder message = new StringBuilder("handled ").append(elems.size()).append(" items in ")
//							.append(printTime(time - lastLog)).append(" total ").append(done).append(" in ")
//							.append(printTime(time - start)).append(" i/s=").append(1000.0 * elems.size() / (time - lastLog))
//							.append(" gi/s=").append(1000.0 * done / (time - start));
//					log.info(message.toString());
//					lastLog = time;
//					savePoint(jobContext);
//					if (elems.size() < fetchSize) {
//						ctn = false;
//					}
//				} else {
//					ctn = false;
//				}
//			}
//			long time = System.currentTimeMillis();
//			long timeElapsed = time - start;
//			String message = "handled " + done + " items in " + printTime(timeElapsed) + " with " + successCount
//					+ " success, " + skipCount + " skips, " + errorCount + " errors";
//			jobContext.addReportFront(message);
//			log.info(message);
//			userTransaction.commit();
//			log.info("last commit in " + printTime(System.currentTimeMillis() - time));
//		} catch (Exception e) {
//			log.error("caught in transaction", e);
//		} finally {
//			try {
//				if (userTransaction.getStatus() == Status.STATUS_ACTIVE
//						|| userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
//					userTransaction.rollback();
//				}
//			} catch (Throwable e) {
//				//should not happen
//				log.error("caught in rollback", e);
//				throw new UnsupportedOperationException(e);
//			}
//		}
//	}
//
//	public void execOnCT(JobContext jobContext, String tableName, Map<String, Object> filters,
//			BiFunction<JobScript.JobContext, Map<String, Object>, HandlingResult> handler, int fetchSize) {
//		execOnCT(jobContext, tableName, filters, handler, fetchSize, null);
//	}
//
//	/**
//	 * list the entries of a CT into a handler. saves after each fetch.
//	 *
//	 * @param jobContext
//	 *          context to write messages into
//	 * @param tableName
//	 *          name of the table to load
//	 * @param filters
//	 *          filters to apply on the fetch
//	 * @param handler
//	 *          what shall we do with each entry
//	 * @param fetchSize
//	 *          size of the list to retrieve on each fetch.
//	 * @param init
//	 *          optional initializer ran inside the transaction. Typically fetches
//	 *          data in the DB.
//	 */
//	public void execOnCT(JobContext jobContext, String tableName, Map<String, Object> filters,
//			BiFunction<JobScript.JobContext, Map<String, Object>, HandlingResult> handler, int fetchSize,
//			Predicate<JobContext> init) {
//		execOnTable(jobContext, tableName, "id", filters, handler, fetchSize, init);
//	}
//
//	public void execOnTable(JobContext jobContext, String tableName, String idField, Map<String, Object> filters,
//			BiFunction<JobScript.JobContext, Map<String, Object>, HandlingResult> handler, int fetchSize,
//			Predicate<JobContext> init) {
//		execOnPaged(
//				jobContext,
//				(m, l) -> {
//					if (l != null) {
//						m.put("fromRangeExclusive " + idField, l);
//					}
//				},
//				idField,
//				pc -> customTableService.list(tableName, pc),
//				filters,
//				l -> lastLine(l, idField),
//				handler,
//				fetchSize,
//				init
//				);
//	}
//
//	protected Long lastLine(List<Map<String, Object>> l, String idField) {
//		return l.stream().mapToLong(last -> {
//			Object field = last.get(idField.toLowerCase());
//			if (field == null) {
//				field = last.get(idField.toUpperCase());
//			}
//			if (field == null) {
//				log.error("no field " + idField + " found, existing are " + last.keySet());
//				return -1l;
//			}
//			return ((Number) field).longValue();
//		}).max().orElse(-1l);
//	}
//
//	public <T extends IEntity> void execOnEntity(JobContext jobContext, PersistenceService<T> persistence,
//			Map<String, Object> filters, BiFunction<JobScript.JobContext, T, HandlingResult> handler, int fetchSize) {
//		execOnEntity(jobContext, persistence, filters, handler, fetchSize, null);
//	}
//
//	public <T extends IEntity> void execOnEntity(JobContext jobContext, PersistenceService<T> persistence,
//			Map<String, Object> filters, BiFunction<JobScript.JobContext, T, HandlingResult> handler, int fetchSize,
//			Predicate<JobContext> init) {
//		execOnPaged(
//				jobContext, (m, l) -> {
//					if (l != null) {
//						m.put("SQL", (m.containsKey("SQL") ? "(" + m.get("SQL") + ") and " : "") + "a.id>" + l);
//					}
//				},
//				"id",
//				pc -> {
//					// persistence.getEntityManager().joinTransaction();
//					return persistence.list(pc);
//				}, filters,
//				l -> l.stream().mapToLong(e->((Number)e.getId()).longValue()).max().orElse(-1l),
//				handler,
//				fetchSize,
//				init
//				);
//	}
//
//	protected String printTime(long millis) {
//		// to 10s
//		if (millis < 10000) {
//			return "" + millis + "ms";
//		}
//
//		long seconds = millis / 1000;
//		// to 5min
//		if (seconds < 300) {
//			return "" + seconds + "s";
//		}
//
//		long minutes = seconds / 60;
//		seconds = seconds % 60;
//		// to 1H
//		if (minutes < 60) {
//			return "" + minutes + "m " + seconds + "s";
//		}
//
//		long hours = minutes / 60;
//		minutes = minutes % 60;
//		// else
//		return "" + hours + "h " + minutes + "m " + seconds + "s";
//	}
//
//	/**
//	 * save local transaction.
//	 *
//	 * @param jobContext
//	 * @return true if save ok. false otherwise.
//	 */
//	protected boolean savePoint(JobScript.JobContext jobContext) {
//		try {
//			userTransaction.commit();
//			userTransaction.begin();
//			return true;
//		} catch (SecurityException | IllegalStateException | RollbackException | HeuristicMixedException
//				| HeuristicRollbackException | SystemException | NotSupportedException e) {
//			jobContext.reportKO("error while making savepoint " + e.getMessage());
//			return false;
//		}
//	}
//
//	/**
//	 * reset local transaction, start a new one.
//	 *
//	 * @param jobContext
//	 * @return
//	 */
//	protected boolean resetPoint(JobScript.JobContext jobContext) {
//		try {
//			userTransaction.rollback();
//			userTransaction.begin();
//			return true;
//		} catch (SecurityException | IllegalStateException | SystemException | NotSupportedException e) {
//			jobContext.reportKO("error while making savepoint " + e.getMessage());
//			return false;
//		}
//	}
//
//	////
//	//
//	////
//
//	/**
//	 * check if a table exists for a given name. if table does not exist, log the
//	 * error and return false
//	 *
//	 * @param tableName
//	 *          name of the table
//	 * @return true if the table exists, false if any error occurs.
//	 */
//	public boolean tableExists(String tableName) {
//		String query = "SELECT count(*) from " + tableName;
//		try {
//			entityManager.createNativeQuery(query).getSingleResult();
//			return true;
//		} catch (Exception e) {
//			Throwable t = e;
//			while (t.getCause() != null) {
//				t = t.getCause();
//			}
//			log.warn("for query " + query + " : " + t.getMessage() + " / " + t, t);
//			return false;
//		}
//	}
//
//	@SuppressWarnings({ "unchecked", "deprecation" })
//	protected List<Map<String, Object>> loadTable(String siplecTableName) {
//		NativeQuery<Map<String, Object>> q = entityManager.unwrap(Session.class)
//				.createSQLQuery("select * from " + siplecTableName);
//		q.setResultTransformer(AliasToEntityOrderedMapResultTransformer.INSTANCE);
//		return q.list();
//	}
//
//	/**
//	 * create a scrollable result on a table with some filtering
//	 *
//	 * @param tableName
//	 * @param filters
//	 * @return
//	 */
//	protected ScrollableResults scrollTable(String tableName, Map<String, Object> filters, int fetchSize) {
//		QueryBuilder queryBuilder = customTableService.getQuery(tableName, new PaginationConfiguration(filters),1L);
//		NativeQuery<?> q = (NativeQuery<?>) queryBuilder.getNativeQuery(customTableService.getEntityManager(), false);
//		ScrollableResults sr = q.setFetchSize(fetchSize).scroll(ScrollMode.SCROLL_INSENSITIVE);
//		return sr;
//	}
//
//	/**
//	 * create a scrollable result on a table with some filtering
//	 *
//	 * @param tableName
//	 * @param filters
//	 * @return
//	 */
//	protected ScrollableResults scrollTable(String tableName, Map<String, Object> filters) {
//		return scrollTable(tableName, filters, 10);
//	}
//
//	/**
//	 * convert a field in a map to a {@link BigInteger}, according to actual type
//	 *
//	 * @param map
//	 *          map to get value from
//	 * @param key
//	 *          key to get the value at
//	 * @return conversion of the existing field into {@link BigInteger}
//	 */
//	protected BigInteger getBigInteger(Map<String, Object> map, String key) {
//		if (key == null) {
//			return null;
//		}
//		Object value = map.get(key.toLowerCase());
//		if (value == null) {
//			return null;
//		}
//		if (value instanceof BigInteger) {
//			return (BigInteger) value;
//		}
//		if (value instanceof BigDecimal) {
//			return ((BigDecimal) value).toBigInteger();
//		}
//		if (value instanceof Number) {
//			// it can be a decimal : get bigDec then convert to BigInt
//			return BigDecimal.valueOf(((Number) value).doubleValue()).toBigInteger();
//		}
//		if (value instanceof String) {
//			return new BigInteger((String) value);
//		}
//		throw new UnsupportedOperationException(
//				"can't load field [" + key + "] type " + value.getClass().getSimpleName() + " as BigInteger");
//	}
//
//	/**
//	 * convert a field in a map to a {@link BigDecimal}, according to actual type
//	 *
//	 * @param map
//	 *          map to get value from
//	 * @param key
//	 *          key to get the value at
//	 * @return conversion of the existing field into {@link BigDecimal}
//	 */
//	protected BigDecimal getBigDecimal(Map<String, Object> map, String key) {
//		if (key == null) {
//			return null;
//		}
//		Object value = map.get(key.toLowerCase());
//		if (value == null) {
//			return null;
//		}
//		if (value instanceof BigDecimal) {
//			return (BigDecimal) value;
//		}
//		if (value instanceof Number) {
//			return BigDecimal.valueOf(((Number) value).doubleValue());
//		}
//		if (value instanceof String) {
//			return new BigDecimal((String) value);
//		}
//		throw new UnsupportedOperationException(
//				"can't load field [" + key + "] type " + value.getClass().getSimpleName() + " as BigDecimal");
//	}
//
//	/**
//	 * convert a field in a map to a {@link Boolean}, according to actual type
//	 *
//	 * @param map
//	 *          map to get value from
//	 * @param key
//	 *          key to get the value at
//	 * @return conversion of the existing field into {@link Boolean}
//	 */
//	protected Boolean getBoolean(Map<String, Object> map, String key) {
//		if (key == null) {
//			return null;
//		}
//		Object value = map.get(key.toLowerCase());
//		if (value == null) {
//			return null;
//		}
//		if (value instanceof Boolean) {
//			return (Boolean) value;
//		}
//		if (value instanceof Number) {
//			return ((Number) value).doubleValue() != 0;
//		}
//		throw new UnsupportedOperationException(
//				"can't load field [" + key + "] type " + value.getClass().getSimpleName() + " as Boolean");
//	}
//
//	protected String getString(Map<String, Object> map, String key) {
//		if (key == null) {
//			return null;
//		}
//		Object value = map.get(key.toLowerCase());
//		if (value == null) {
//			return null;
//		}
//		if (value instanceof String) {
//			return (String) value;
//		}
//		throw new UnsupportedOperationException(
//				"can't load field [" + key + "] type " + value.getClass().getSimpleName() + " as String");
//	}
//
//	protected Date getDate(Map<String, Object> map, String key) {
//		if (key == null) {
//			return null;
//		}
//		Object value = map.get(key.toLowerCase());
//		if (value == null) {
//			return null;
//		}
//		if (value instanceof Date) {
//			return (Date) value;
//		}
//		throw new UnsupportedOperationException(
//				"can't load field [" + key + "] type " + value.getClass().getSimpleName() + " as Date");
//	}
//
//	//
//	// dir management
//	//
//
//	/**
//	 * ensure the dir is present from root path of application
//	 *
//	 * @param pathFromRoot path from application's root dir
//	 * @param onError handler on error
//	 * @return such a File, if exist or is created ; null if can't exist. Also
//	 *         calls the onError if can't exist.
//	 */
//	public File requireDir(String pathFromRoot, Consumer<String> onError) {
//		String rootPath = ParamBean.getInstance().getChrootDir(providerService.getProvider().getCode());
//		return requireDir(new File(rootPath), pathFromRoot, onError);
//	}
//
//	/**
//	 * ensure the dir is present from root path of application
//	 *
//	 * @param pathFromRoot path from application's root dir
//	 * @param context
//	 *          context to add a KO on error.
//	 * @return such a File, if exist or is created ; null if can't exist. Also
//	 *         adds a KO in the context if can't exist.
//	 */
//	public File requireDir(String pathFromRoot, JobContext context) {
//		String rootPath = ParamBean.getInstance().getChrootDir(providerService.getProvider().getCode());
//		return requireDir(new File(rootPath), pathFromRoot, context);
//	}
//
//	/**
//	 * ensure the dir is present from root path of application
//	 *
//	 * @param pathFromRoot
//	 * @param context
//	 *          context to add a KO on error.
//	 * @return such a File, if exist or is created ; null if can't exist. Also
//	 *         adds a KO in the context if can't exist.
//	 */
//	public File requireDir(String pathFromRoot, WSContext context) {
//		String rootPath = ParamBean.getInstance().getChrootDir(providerService.getProvider().getCode());
//		return requireDir(new File(rootPath), pathFromRoot, context);
//	}
//
//	/**
//	 *
//	 * ensure the dir is present as a child from its parent
//	 *
//	 * @param parentFile
//	 *          parent file, typical already checked
//	 * @param childPath
//	 *          path from the parent. can be depth >1 eg a/b/c
//	 * @param onError
//	 *          called when an error is found
//	 * @return
//	 */
//	public File requireDir(File parentFile, String childPath, Consumer<String> onError) {
//		File ret = new File(parentFile, childPath);
//		if (!ret.exists()) {
//			if (!ret.mkdirs()) {
//				onError.accept("can't create dir " + ret.getAbsolutePath());
//				return null;
//			}
//		}
//		if (!ret.isDirectory()) {
//			onError.accept("file already exists and is not a dir " + ret.getAbsolutePath());
//			return null;
//		}
//		return ret;
//	}
//
//	/**
//	 *
//	 * ensure the dir is present as a child from its parent
//	 *
//	 * @param parentFile
//	 *          parent file, typical already checked
//	 * @param childPath
//	 *          path from the parent. can be depth >1 eg a/b/c
//	 * @param context
//	 *          context to add a KO on error.
//	 * @return
//	 */
//	public File requireDir(File parentFile, String childPath, JobContext context) {
//		return requireDir(parentFile, childPath, context::reportKO);
//	}
//
//	/**
//	 *
//	 * ensure the dir is present as a child from its parent
//	 *
//	 * @param parentFile
//	 *          parent file, typical already checked
//	 * @param childPath
//	 *          path from the parent. can be depth >1 eg a/b/c
//	 * @param context
//	 *          context to return a 500 on error
//	 * @return
//	 */
//	public File requireDir(File parentFile, String childPath, WSContext context) {
//		return requireDir(parentFile, childPath, s -> context.setTextResponse(500, s));
//	}
//
//}
