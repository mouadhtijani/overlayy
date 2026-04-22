//package eec.epi;
//
//import org.meveo.model.DatePeriod;
//import org.meveo.model.ICustomFieldEntity;
//import org.meveo.model.crm.custom.CustomFieldValue;
//import org.meveo.model.crm.custom.CustomFieldValues;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.List;
//import java.util.function.Consumer;
//
//public class EntityUpdateTools {
//
//	/**
//	 * test if a new value is not null and an update on an old one. This is here
//	 * to reduce code verbosity
//	 *
//	 * @param <T>
//	 *          consider it Object
//	 * @param newVal
//	 *          the new value we received
//	 * @param oldVal
//	 *          the old value we had
//	 * @return true if newVal not null and using newVal instead of oldVal is a
//	 *         change
//	 */
//	public static <T> boolean needUpdate(T newVal, T oldVal) {
//		return newVal != null && (oldVal == null || !newVal.equals(oldVal));
//	}
//
//	/**
//	 * update a CF on a target, if required.
//	 *
//	 * @param <T>
//	 *          type of CF
//	 * @param target
//	 *          the target to modify
//	 * @param cfName
//	 *          name of the CF
//	 * @param newVal
//	 *          the newValue to use
//	 * @return true if the newVal was not null and its assignment as cfName
//	 *         produced a change.
//	 */
//	public static <T> boolean updateCF(ICustomFieldEntity target, String cfName, T newVal) {
//		if (newVal == null) {
//			return false;
//		}
//		if (needUpdate(newVal, target.getCfValue(cfName))) {
//			target.setCfValue(cfName, newVal);
//			return true;
//		} else {
//			return false;
//		}
//	}
//
//	/**
//	 * update a field on an entity, if required. This is here to reduce code
//	 * verbosity
//	 *
//	 * @param <T>
//	 *          type of field
//	 * @param newVal
//	 *          the new value to assign if not null
//	 * @param oldVal
//	 *          the old value present
//	 * @param setter
//	 *          function to update the entity's field
//	 * @return
//	 */
//	public static <T> boolean updateVal(T newVal, T oldVal, Consumer<T> setter) {
//		if (needUpdate(newVal, oldVal)) {
//			setter.accept(newVal);
//			return true;
//		}
//		return false;
//	}
//
//	/**
//	 * add versioned value on an entity if required. The value is added if not
//	 * null, and the adding results in a change
//	 *
//	 * @return true iff the adding resulted in a change
//	 */
//	public static boolean addValueIFRequired(ICustomFieldEntity entity, String cfCode, Object value, Date startDate,
//			Date endDate) {
//		return addValueIFRequired(entity.getCfValuesNullSafe(), cfCode, value, startDate, endDate);
//	}
//
//	/**
//	 * add versioned value on an entity's cfValues if required. The value is added
//	 * if not null, and the adding results in a change. This added for tests, as
//	 * testing the public method
//	 * {@link #addValueIFRequired(ICustomFieldEntity, String, Object, Date, Date)}
//	 * is impossible wihout a container.
//	 *
//	 * @return true iff the adding resulted in a change
//	 */
//	static boolean addValueIFRequired(CustomFieldValues cfValues, String cfCode, Object value, Date startDate,
//			Date endDate) {
//		if (value == null) {
//			return false;
//		}
//		List<CustomFieldValue> customFieldValues = cfValues.getValuesByCode().get(cfCode);
//		// If no cf add
//		if (customFieldValues == null || customFieldValues.isEmpty()) {
//			cfValues.setValue(cfCode, new DatePeriod(startDate, endDate), 0, value);
//			return true;
//		}
//		// if exists return
//		for (CustomFieldValue customFieldValue : customFieldValues) {
//			if (value.equals(customFieldValue.getValue())) {
//				DatePeriod period = customFieldValue.getPeriod();
//				if (period == null) {
//					if (startDate == null && endDate == null) {
//						return false;
//					}
//				} else {
//					if (period.getFrom() == null && startDate == null
//							|| period.getFrom() != null && period.getFrom().equals(startDate)) {
//						if (period.getTo() == null && endDate == null || period.getTo() != null && period.getTo().equals(endDate)) {
//							return false;
//						}
//					}
//				}
//			} else {
//			}
//		}
//		// check if all existing periods would not be impacted by this change. if no
//		// change return false
//		List<Date[]> periodsWithImpacts = new ArrayList<>();
//		periodsWithImpacts.add(new Date[] { startDate, endDate });
//		List<CustomFieldValue> sorted = new ArrayList<>(customFieldValues);
//		sorted.sort(Comparator.comparing(cf -> -cf.getPriority()));
//		for (CustomFieldValue cf : sorted) {
//			// if CF has same value, we remove all its periods
//			if (value.equals(cf.getValue())) {
//				List<Date[]> remove = new ArrayList<>();
//				List<Date[]> add = new ArrayList<>();
//				for (Date[] impactedPeriod : periodsWithImpacts) {
//					if (touchPeriod(cf.getPeriod(), impactedPeriod)) {
//						remove.add(impactedPeriod);
//						if (cf.getPeriod() != null && (cf.getPeriod().getFrom() != null || cf.getPeriod().getTo() != null)) {
//							Date cfStart = cf.getPeriod().getFrom();
//							Date impactedStart = impactedPeriod[0];
//							Date cfEnd = cf.getPeriod().getTo();
//							Date impactedEnd = impactedPeriod[1];
//							if (impactedStart == null && cfStart != null
//									|| impactedStart != null && cfStart != null && impactedStart.before(cfStart)) {
//								add.add(new Date[] { impactedStart, cfStart });
//							}
//							if (impactedEnd == null && cfEnd != null
//									|| impactedEnd != null && cfEnd != null && impactedEnd.after(cfEnd)) {
//								add.add(new Date[] { cfEnd, impactedEnd });
//							}
//						}
//					}
//				}
//				periodsWithImpacts.removeAll(remove);
//				periodsWithImpacts.addAll(add);
//				if (periodsWithImpacts.isEmpty()) {
//					return false;
//				}
//			} else {
//				// if CF has different value that the one we add and it touches a period
//				// we want to modify, we need to add
//				if (periodsWithImpacts.stream().anyMatch(o -> touchPeriod(cf.getPeriod(), o))) {
//					break;
//				} else {
//				}
//			}
//		}
//		// else add
//		int newPrio = customFieldValues.stream().mapToInt(cfv -> cfv.getPriority()).max().orElse(-1) + 1;
//		customFieldValues.add(new CustomFieldValue(new DatePeriod(startDate, endDate), newPrio, value));
//		// cfValues.setValue(cfCode, new DatePeriod(startDate, endDate), -1, value);
//		return true;
//	}
//
//	static boolean touchPeriod(DatePeriod datePeriod, Date[] newPeriod) {
//		if (datePeriod == null) {
//			return true;
//		}
//		Date start0 = datePeriod.getFrom();
//		Date start1 = newPeriod[0];
//		Date end0 = datePeriod.getTo();
//		Date end1 = newPeriod[1];
//		return touchPeriod(start0, end0, start1, end1);
//	}
//
//	/**
//	 * check if two periods, represented each as [from, to[ , have common dates
//	 *
//	 * @param from0
//	 *          period 0 start date, included
//	 * @param to0
//	 *          period 0 end date, excluded
//	 * @param from1
//	 *          period 1 start date, included
//	 * @param to1
//	 *          period 1 end date, excluded
//	 * @return true when there is at least one date that belongs to both period
//	 */
//	static boolean touchPeriod(Date from0, Date to0, Date from1, Date to1) {
//		// if a period is -inf, +inf they touch
//		if (from0 == null && to0 == null || from1 == null && to1 == null) {
//			return true;
//		}
//		// if both period are from -in or both are to +inf they touch
//		if (from0 == null && from1 == null || to0 == null && to1 == null) {
//			return true;
//		}
//		if (from1 == null || from0 != null && from1.before(from0)) {
//			// if period 1 starts before period 0, they match when period 1 ends after
//			// period 0 starts
//			return to1 == null || to1.after(from0);
//		} else {
//			// if period 1 starts same or after period 0, they match when period 0
//			// ends after or same period 1 start
//			return to0 == null || to0.after(from1);
//		}
//	}
//
//}
