//package eec.epi;
//
//import org.hibernate.ScrollMode;
//import org.hibernate.ScrollableResults;
//import org.hibernate.internal.SessionImpl;
//import org.hibernate.query.NativeQuery;
//import org.meveo.admin.exception.BusinessException;
//import org.meveo.admin.util.pagination.PaginationConfiguration;
//import org.meveo.commons.utils.QueryBuilder;
//import org.meveo.model.transformer.AliasToEntityOrderedMapResultTransformer;
//import org.meveo.service.custom.CustomTableService;
//
//import javax.persistence.EntityManager;
//import java.math.BigDecimal;
//import java.math.BigInteger;
//import java.sql.DatabaseMetaData;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.function.BiFunction;
//
///**
// * common class for entries in a Custom Table.
// *
// *
// */
//public abstract class CTEntry {
//
//	/**
//	 * the name/code the CT is stored as.
//	 *
//	 * @return
//	 */
//	public abstract String getCTName();
//
//	/**
//	 * internal fields
//	 */
//	protected final Map<String, Object> fields;
//
//	/**
//	 * service used to {@link #save()} this.
//	 */
//	protected final CustomTableService customTableService;
//
//	@Override
//	public String toString() {
//		return getClass().getName() + "{" + "fields=" + fields + '}';
//	}
//
//	public CTEntry(CustomTableService customTableService, Map<String, Object> fields) {
//		this.fields = fields;
//		this.customTableService = customTableService;
//	}
//
//	public void save() throws BusinessException {
//		if (id() != null) {
//			customTableService.update(getCTName(), fields, false);
//		} else {
//			customTableService.create(getCTName(), java.util.Arrays.asList(fields), false);
//			fields.put("id", id());
//		}
//	}
//
//	protected BigInteger getBigInteger(String fieldName) {
//		Object ret = fields.get(fieldName);
//		if (ret == null) {
//			return null;
//		}
//		if (ret instanceof BigInteger) {
//			return (BigInteger) ret;
//		}
//		if (ret instanceof BigDecimal) {
//			return ((BigDecimal) ret).toBigInteger();
//		}
//		if (ret instanceof Number) {
//			return BigInteger.valueOf(((Number) ret).longValue());
//		}
//		throw new UnsupportedOperationException("can't cast class " + ret.getClass().getSimpleName() + " into Biginteger");
//	}
//
//	protected Boolean getBoolean(String fieldName) {
//		Object ret = fields.get(fieldName);
//		if (ret == null) {
//			return null;
//		}
//		if (ret instanceof Boolean) {
//			return (Boolean) ret;
//		}
//		if (ret instanceof Number) {
//			return ((Number) ret).intValue() != 0;
//		}
//		if (ret instanceof String) {
//			String ret_s = (String) ret;
//			if (ret_s.isBlank()) {
//				return null;
//			}
//			if ("true".equalsIgnoreCase(ret_s)) {
//				return Boolean.TRUE;
//			}
//			if ("false".equalsIgnoreCase(ret_s)) {
//				return Boolean.FALSE;
//			}
//			throw new UnsupportedOperationException("can't convert string " + ret_s + " into Boolean");
//		}
//		throw new UnsupportedOperationException("can't cast class " + ret.getClass().getSimpleName() + " into Boolean");
//	}
//
//	protected BigDecimal getBigDecimal(String fieldName) {
//		Object ret = fields.get(fieldName);
//		if (ret == null) {
//			return null;
//		}
//		if (ret instanceof BigDecimal) {
//			return (BigDecimal) ret;
//		}
//		if (ret instanceof BigInteger) {
//			return new BigDecimal((BigInteger) ret);
//		}
//		if (ret instanceof Number) {
//			return BigDecimal.valueOf(((Number) ret).doubleValue());
//		}
//		throw new UnsupportedOperationException("can't cast class " + ret.getClass().getSimpleName() + " into BigDecimal");
//	}
//
//	public BigInteger id() {
//		return getBigInteger("id");
//	}
//
//	/**
//	 * get a sublist of the items in a table.
//	 *
//	 * @param entityManager
//	 * @param siplecTableName
//	 * @param ids
//	 *          the ids we want to limit the result to.
//	 * @return new List, or empty if ids is null/empty.
//	 */
//	@SuppressWarnings({ "unchecked", "deprecation" })
//	public static List<Map<String, Object>> loadTable(EntityManager entityManager, String siplecTableName, long... ids) {
//		if (ids == null || ids.length == 0) {
//			return Collections.emptyList();
//		}
//		StringBuilder whereClauseSB = null;
//		for (long l : ids) {
//			if (whereClauseSB == null) {
//				whereClauseSB = new StringBuilder(" where id in (");
//			} else {
//				whereClauseSB = whereClauseSB.append(",");
//			}
//			whereClauseSB = whereClauseSB.append(l);
//		}
//		whereClauseSB.append(") ");
//		NativeQuery<Map<String, Object>> q = entityManager.unwrap(SessionImpl.class)
//				.createSQLQuery("select * from " + siplecTableName + whereClauseSB.toString());
//		q.setResultTransformer(AliasToEntityOrderedMapResultTransformer.INSTANCE);
//		return q.list();
//	}
//
//	/**
//	 *
//	 * @param <T>
//	 * @param customTableService
//	 * @param tableName
//	 * @param filters
//	 * @param constructor
//	 *          typically T::new
//	 * @param fetchEvery
//	 *          number of entries that are fetched by the db on each group.
//	 * @return
//	 */
//	public static <T extends CTEntry> Entryterator<T> scrollTable(CustomTableService customTableService, String tableName,
//			Map<String, Object> filters, BiFunction<CustomTableService, Map<String, Object>, T> constructor,
//			Integer fetchEvery) {
//		ScrollableResults internal = scrollTable(customTableService, tableName, filters, fetchEvery);
//		ArrayList<String> columns = columns(customTableService.getEntityManager(), tableName);
//		return new Entryterator<>(internal, constructor, customTableService, columns);
//	}
//
//	/**
//	 * create a scrollable result on a table with some filtering
//	 *
//	 * @param customTableService
//	 * @param tableName
//	 * @param filters
//	 * @return
//	 */
//	protected static ScrollableResults scrollTable(CustomTableService customTableService, String tableName,
//			Map<String, Object> filters, Integer fetchSize) {
//		if (fetchSize == null) {
//			fetchSize = 10;
//		}
//		QueryBuilder queryBuilder = customTableService.getQuery(tableName, new PaginationConfiguration(filters));
//		NativeQuery<?> q = (NativeQuery<?>) queryBuilder.getNativeQuery(customTableService.getEntityManager(), false);
//		ScrollableResults sr = q.setFetchSize(fetchSize).scroll(ScrollMode.SCROLL_INSENSITIVE);
//		return sr;
//	}
//
//	/**
//	 * list the columns in a table
//	 *
//	 * @param entityManager
//	 * @param tableName
//	 * @return
//	 */
//	protected static ArrayList<String> columns(EntityManager entityManager, String tableName) {
//		ArrayList<String> allFields = new ArrayList<>();
//		DatabaseMetaData metadata;
//		try {
//			metadata = entityManager.unwrap(SessionImpl.class).connection().getMetaData();
//			ResultSet rs = metadata.getColumns(null, null, tableName, null);
//			while (rs.next()) {
//				allFields.add(rs.getString("COLUMN_NAME"));
//			}
//			if (allFields.isEmpty()) {
//				// we try again with uppercase table name. That means the DB is
//				// potentially case insensitive, so we return the lowercase fields
//				String uppercase = tableName.toUpperCase();
//				metadata = entityManager.unwrap(SessionImpl.class).connection().getMetaData();
//				rs = metadata.getColumns(null, null, uppercase, null);
//				while (rs.next()) {
//					allFields.add(rs.getString("COLUMN_NAME").toLowerCase());
//				}
//				if (allFields.isEmpty()) {
//					String noSchemaTableName = tableName.contains(".") ? tableName.replaceAll(".*\\.", "") : tableName;
//					ResultSet tables = metadata.getTables(null, null, null, null);
//					List<String> matching = new ArrayList<>();
//					while (tables.next()) {
//						String name = tables.getString("TABLE_NAME");
//						String schema = tables.getString("TABLE_SCHEM");
//						if (noSchemaTableName.compareToIgnoreCase(name) == 0) {
//							if (schema != null && schema.length() > 0) {
//								matching.add(schema + "." + name);
//							} else {
//								matching.add(name);
//							}
//						}
//					}
//					throw new UnsupportedOperationException(
//							"empty columns for table " + tableName + " corresponding tables are " + matching);
//				}
//			}
//		} catch (SQLException e) {
//			throw new UnsupportedOperationException("catch this", e);
//		}
//		return allFields;
//	}
//
//}
