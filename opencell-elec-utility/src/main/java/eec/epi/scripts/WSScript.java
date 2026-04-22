package eec.epi.scripts;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.notification.InboundRequest;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public abstract class WSScript extends ACommonScript {

	static String extractType(String contentType) {
		return contentType == null ? null : contentType.replaceAll(";.*", "").replaceAll(".*/", "").trim().toLowerCase();
	}

	static Object valuePath(Object source, String path) {
		if (source == null) {
			return null;
		}
		String[] paths = path.split("\\.");
		for (String item : paths) {
			source = valueOf(source, item);
			if (source == null) {
				return null;
			}
		}
		return source;
	}

	static Object valueOf(Object source, String field) {
		if (source instanceof Map) {
			Object ret = ((Map<?, ?>) source).get(field);
			if (ret == null) {
				Map<Object, Object> map = ((Map<?, ?>) source).entrySet().stream()
						.filter(e -> String.valueOf(e.getKey()).startsWith(field + "."))
						.collect(
								Collectors.toMap(e -> String.valueOf(e.getKey()).substring(field.length() + 1), e -> e.getValue()));
				if (map.size() > 0) {
					ret = map;
				}
			}
			return ret;
		} else {
			try {
				Field f = source.getClass().getField(field);
				if (f == null) {
					return null;
				}
				return f.get(source);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				return null;
			}
		}
	}

	public static class WSContext {

		public final Map<String, Object> methodContext;
		public final InboundRequest inboundRequest;
		protected transient Map<String, Object> bodyJson = null;

		public Map<String, Object> getMethodContext() {
			return methodContext;
		}

		public WSContext(Map<String, Object> methodContext, String requestParam) {
			this.methodContext = Objects.requireNonNull(methodContext);
			inboundRequest = (InboundRequest) methodContext.get(requestParam);
			bodyJson = bodyOf(new TypeReference<Map<String, Object>>() {
			});
		}

		public void setResponse(int code, String contentType, String body) {
			inboundRequest.setResponseStatus(code);
			inboundRequest.setResponseContentType(contentType);
			inboundRequest.setResponseBody(body);
		}

		public void setResponse(int code, String contentType, String body, Charset charset) {
			inboundRequest.setResponseStatus(code);
			inboundRequest.setResponseContentType(contentType);
			inboundRequest.setBytes(body.getBytes(charset));
		}

		public void setJSonResponse(int code, String body) {
			setResponse(code, "application/json", body);
		}

		public void setJSonOK(String result) {
			setJSonResponse(200, result);
		}

		public void setTextResponse(int code, String body) {
			setResponse(code, "text/plain", body);
		}

		public void setTextOK(String result) {
			setTextResponse(200, result);
		}

		public static class PortalResult {

			protected HashMap<String, Object> fields = new HashMap<>();
			protected int responseCode = 200;

			public PortalResult() {
			}

			public static PortalResult error(int errorCode, String errorMessage) {
				PortalResult ret = new PortalResult();
				ret.responseCode = errorCode;
				ret.addField("status", "ERROR");
				ret.addField("errorCode", errorCode);
				ret.addField("message", errorMessage);
				return ret;
			}

			public static PortalResult success() {
				PortalResult ret = new PortalResult();
				ret.addField("status", "SUCCESS");
				return ret;
			}

			public static PortalResult success(int responseCode) {
				PortalResult ret = new PortalResult();
				ret.responseCode = responseCode;
				ret.addField("status", "SUCCESS");
				return ret;
			}

			/**
			 * create a portalResult to transmit an array back. Checks if each item of
			 * the array has an "id" field of type number, required for transmission.
			 *
			 * @param it
			 *          iterable of data (can be array, Collection, etc)
			 * @return a portal result to transmit. Note that if an item of the array
			 *         misses its id, then an error response is returned instead.
			 */
			public static PortalResult successDataArr(Iterable<?> it) {
				PortalResult ret = success(200);
				List<String> errors = new ArrayList<>();
				for (Object o : it) {
					String error = null;
					if (o == null) {
						error = "response contains null data";
						LoggerFactory.getLogger(WSContext.class).error(error + " " + it, new NullPointerException());
					} else {
						try {
							Field idf = o.getClass().getField("id");
							Object id = idf.get(o);
							if (id == null) {
								error = "item " + o + "contains null id";
								LoggerFactory.getLogger(WSContext.class).error(error, new NullPointerException());
							}
						} catch (NoSuchFieldException | SecurityException e) {
							error = "can't find field \"id\" in class " + o.getClass().getSimpleName() + " of item" + o;
						} catch (IllegalArgumentException | IllegalAccessException e) {
							error = "can't access id of item " + o;
						}
					}
					if (error != null) {
						errors.add(error);
					}
				}
				if (errors.isEmpty()) {
					ret.addField("data", it);
				} else {
					ret = error(500, "errors when processing response : " + errors);
				}
				return ret;
			}

			public boolean isError() {
				return responseCode / 100 != 2 && responseCode != 304;
			}

			public PortalResult addField(String fieldName, Object value) {
				fields.put(fieldName, value);
				return this;
			}

			protected String toJson() {
				ObjectMapper mapper = new ObjectMapper();
				mapper.setSerializationInclusion(Include.NON_NULL);
				ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
				try {
					return ow.writeValueAsString(fields);
				} catch (JsonProcessingException e) {
					throw new UnsupportedOperationException(e);
				}
			}
		}

		public void setPortalResponse(PortalResult result) {
			setJSonResponse(result.responseCode, result.toJson());
		}

		public void setPortalError(int errorCode, String message) {
			setPortalResponse(PortalResult.error(errorCode, message));
		}

		public void setPortalError(HandlingResult h) {
			int code = 500;
			if (h.code != null) {
				try {
					code = Integer.parseInt(h.code);
				} catch (Exception e) {
				}
			}
			setPortalResponse(PortalResult.error(code, h.error));
		}

		public String param(String key) {
			if (key == null) {
				return null;
			}
			String ret = inboundRequest.getParameters().get(key);
			if (ret != null) {
				return ret;
			}
			if (bodyJson != null) {
				Object bodyRet = bodyJson.get(key);
				if (bodyRet == null) {
					bodyRet = valuePath(bodyJson, key);
				}
				if (bodyRet != null) {
					return String.valueOf(bodyRet);
				}

			}
			return null;
		}

		public String param(String key, String def) {
			String ret = param(key);
			return ret != null ? ret : def;
		}

		public void header(String key, String value) {
			if (key == null) {
				throw new NullPointerException();
			}
			if (value == null) {
				inboundRequest.getResponseHeaders().remove(key);
			} else {
				inboundRequest.getResponseHeaders().put(key, value);
			}
		}

		/**
		 * parse the body into a given class. internal exception are caught and
		 * thrown back. use {@link #bodyOf(Class)} when possible, this one is made
		 * for collections.
		 *
		 * @param <T>
		 *          class to parse the body into
		 * @param ref
		 *          typereference to use.
		 * @return body parsed
		 */
		public <T> T bodyOf(TypeReference<T> ref) {
			String ctType = extractType(inboundRequest.getContentType());
			if (ctType != null) {
				switch (ctType) {
				case "json":
					ObjectMapper om = new ObjectMapper();
					try {
						return om.readValue(inboundRequest.getBody(), ref);
					} catch (JsonProcessingException e) {
						throw new UnsupportedOperationException("while processing json of " + inboundRequest.getBody(), e);
					}
				default:
					LoggerFactory.getLogger(WSScript.class)
					.warn("no param mapping for content-type short=" + ctType + " full=" + inboundRequest.getContentType());
				}
			}
			return null;
		}

		public <T> T bodyOf(Class<T> ref) {
			return bodyOf(new TypeReference<T>() {
			});
		}
	}

	@Override
	public void execute(Map<String, Object> methodContext) throws BusinessException {
		execute(new WSContext(methodContext, getRequestParam()));
	}

	public abstract void execute(WSContext wsContext);

	/**
	 * overwrite to change the parameter the event is named to in the collection.
	 *
	 * @return the parameter to get the inboundrequest from.
	 */
	public String getRequestParam() {
		return "CONTEXT_ENTITY";
	}
}
