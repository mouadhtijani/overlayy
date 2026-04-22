//package eec.epi;
//
//import org.hibernate.ScrollableResults;
//import org.meveo.service.custom.CustomTableService;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.BiFunction;
//import java.util.function.Supplier;
//
///**
// * <p>
// * converter on a scrollable result to the transformed items.
// * </p>
// * <p>
// * transformation puts every colmuns in a map then calls a constructor on the
// * map. map is generated on the stack but can be reused by assigning it with
// * the {@link #setMapSupplier(Supplier)} (think of clearing it)
// * </p>
// * <p>
// * Object is both iterable and iterator, so it can easily be used in a for.
// * iterable returns this as the iterator, so can't be reused
// * </p>
// * <p>
// * auto closeable to close the underlying {@link ScrollableResults}
// * </p>
// * <p>
// * usage : <pre>
// * try(CTIterator<T> it = createIterator())
// * {
// *   // not mandatory. Here we reuse the map every time
// *   Map<String, Object> map = new HashMap<>();
// *   it.setMapSupplier(()->{map.clear(); return map;});
// *   //
// *   for( T t : it){
// *     processItem(t);
// *   }
// * }
// * </pre>
// * </p>
// *
// *
// * @param <T>
// *          CT representation class.
// */
//public class Entryterator<T extends CTEntry> implements Iterator<T>, Iterable<T>, AutoCloseable {
//
//	private final ScrollableResults internal;
//	private final BiFunction<CustomTableService, Map<String, Object>, T> constructor;
//	private final CustomTableService customTableService;
//	private final ArrayList<String> columns;
//
//	/**
//	 *
//	 * @param internal
//	 *          the scroll of result, typically made with
//	 * @param constructor
//	 * @param customTableService
//	 * @param columns
//	 */
//	public Entryterator(ScrollableResults internal, BiFunction<CustomTableService, Map<String, Object>, T> constructor,
//			CustomTableService customTableService, ArrayList<String> columns) {
//		this.internal = Objects.requireNonNull(internal);
//		this.constructor = Objects.requireNonNull(constructor);
//		this.customTableService = Objects.requireNonNull(customTableService);
//		this.columns = Objects.requireNonNull(columns);
//	}
//
//	protected Supplier<Map<String, Object>> mapSupplier = () -> new HashMap<>();
//
//	protected void setMapSupplier(Supplier<Map<String, Object>> mapSupplier) {
//		this.mapSupplier = mapSupplier;
//	}
//
//	private Object[] next = null;
//
//	@Override
//	public boolean hasNext() {
//		if (next == null && internal.next()) {
//			next = internal.get();
//		}
//		return next != null;
//	}
//
//	@Override
//	public T next() {
//		if (!hasNext()) {
//			throw new UnsupportedOperationException("calling next without next ; last is " + internal.isLast());
//		}
//		Map<String, Object> entry = Objects.requireNonNull(mapSupplier.get());
//		for (int i = 0;
//				i < next.length
//				&& i < columns.size();
//				i++) {
//			entry
//			.put(
//					columns
//					.get(i),
//					next[i]
//					);
//		}
//		next = null;
//		return constructor.apply(customTableService, entry);
//	}
//
//	@Override
//	public Iterator<T> iterator() {
//		return this;
//	}
//
//	@Override
//	public void close() {
//		internal.close();
//	}
//
//	public static class Chain<T extends CTEntry> implements Iterator<T>, Iterable<T>, AutoCloseable {
//
//		private final List<Entryterator<T>> elems;
//
//		public Chain(List<Entryterator<T>> elems) {
//			this.elems = elems;
//		}
//
//		@SafeVarargs
//		public Chain(Entryterator<T>... elems) {
//			this.elems = new ArrayList<>(List.of(elems));
//		}
//
//		@Override
//		public void close() throws Exception {
//			for (Entryterator<T> ct : elems) {
//				ct.close();
//			}
//		}
//
//		@Override
//		public Iterator<T> iterator() {
//			return this;
//		}
//
//		@Override
//		public boolean hasNext() {
//			while (!elems.isEmpty() && !elems.get(0).hasNext()) {
//				elems.remove(0);
//			}
//			return !elems.isEmpty() && elems.get(0).hasNext();
//		}
//
//		@Override
//		public T next() {
//			return hasNext() ? elems.get(0).next() : null;
//		}
//
//	}
//
//}