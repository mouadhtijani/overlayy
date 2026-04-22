//package eec.epi;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import org.junit.Assert;
//import org.junit.Test;
//import org.meveo.api.dto.CustomEntityTemplateDto;
//import org.meveo.api.dto.CustomFieldTemplateDto;
//import org.meveo.model.crm.custom.CustomFieldTypeEnum;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
//
//import eec.epi.maven.plugins.PostmanCollectionv2_1;
//import eec.epi.maven.plugins.PostmanCollectionv2_1.Event;
//import eec.epi.maven.plugins.PostmanCollectionv2_1.Item;
//
//public class TestDevCollections {
//
//	@Test
//	public void verifyCollections() {
//		File dir = new File("dev");
//		if (dir.exists() && dir.isDirectory()) {
//			List<String> errors = checkFileOrDir(dir);
//			Assert.assertEquals(Collections.emptyList(), errors);
//		}
//	}
//
//	protected static List<String> checkFileOrDir(File file) {
//		if (file.isFile()) {
//			return checkBinaryCollection(file);
//		} else {
//			List<String> ret = new ArrayList<>();
//			for (File child : file.listFiles()) {
//				ret.addAll(checkFileOrDir(child));
//			}
//			return ret;
//		}
//	}
//
//	protected static List<String> checkBinaryCollection(File file) {
//		if (!file.getName().endsWith("postman_collection.json")) {
//			return Collections.emptyList();
//		} else {
//			PostmanCollectionv2_1 coll;
//			try {
//				coll = PostmanCollectionv2_1.loadCollectionFile(file);
//			} catch (Exception e) {
//				return Arrays.asList(e.getClass().getSimpleName() + " : " + e.getMessage());
//			}
//			return checkCollection(coll);
//		}
//	}
//
//	protected static List<String> checkCollection(PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		ret.addAll(checkAuth(coll));
//		ret.addAll(checkTests(coll));
//		ret.addAll(checkCTRequests(coll));
//		ret.addAll(checkCFRequests(coll));
//		return ret;
//	}
//
//	///
//	// check auth in collections, sub folders and requests
//	///
//
//	protected static final Map<String, String> USERPASS = Map.of(
//		 "{{opencell.username}}","{{opencell.password}}"
//		,"{{opencell.superuser}}","{{opencell.superpass}}"
//	);
//
//	protected static List<String> checkAuth(PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		if (coll.auth.basic == null) {
//			ret.add("coll " + coll.info.name + " : missing basic auth");
//		} else {
//			String expectedpass=null;
//			{// check basic username
//				List<String> usernames = coll.auth.basic.stream().filter(h -> "username".equals(h.key)).map(h -> h.value)
//						.collect(Collectors.toList());
//				if (usernames.size() != 1) {
//					ret.add("coll " + coll.info.name + " : auth basic invalid username =" + usernames);
//				} else if (!USERPASS.containsKey(usernames.get(0))) {
//					ret.add("coll " + coll.info.name + " : auth basic requests " + USERPASS.keySet() + " username, is" + usernames);
//				} else {
//					expectedpass =USERPASS.get(usernames.get(0));
//				}
//			}
//			if(expectedpass!=null)
//			{// check basic password
//				List<String> passwords = coll.auth.basic.stream().filter(h -> "password".equals(h.key)).map(h -> h.value)
//						.collect(Collectors.toList());
//				if (passwords.size() != 1) {
//					ret.add("coll " + coll.info.name + " : auth basic invalid pasword =" + passwords);
//				} else if (!expectedpass.equals(passwords.get(0))) {
//					ret.add("coll " + coll.info.name + " : auth basic requests " + expectedpass + " password, is" + passwords);
//				}
//			}
//		}
//		for (Item it : coll.item) {
//			ret.addAll(checkAuth(it, coll));
//		}
//		return ret;
//	}
//
//	protected static List<String> checkAuth(Item it, PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		if (it.auth != null && it.auth.basic != null && !it.auth.basic.isEmpty()) {
//			ret.add("coll " + coll.info.name + " : auth basic forbidden in item =" + it.name);
//		}
//		if (it.request != null && it.request.auth != null && it.request.auth.basic != null
//				&& !it.request.auth.basic.isEmpty()) {
//			ret.add("coll " + coll.info.name + " : auth basic forbidden in request of item =" + it.name);
//		}
//		if (it.item != null) {
//			for (Item child : it.item) {
//				ret.addAll(checkAuth(child, coll));
//			}
//		}
//		return ret;
//	}
//
//	///
//	// check the absence of tests that can fail the requests
//	///
//
//	protected static List<String> checkTests(PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		if (coll.event != null && !coll.event.isEmpty()) {
//			List<Event> testEvents = coll.event.stream().filter(event -> "test".equals(event.listen))
//					.collect(Collectors.toList());
//			for (Event e : testEvents) {
//				String error = checkTests(e, coll, null);
//				if (error != null) {
//					ret.add(error);
//				}
//			}
//		}
//		if (coll.item != null) {
//			for (Item it : coll.item) {
//				ret.addAll(checkTests(it, coll));
//			}
//		}
//		return ret;
//	}
//
//	protected static String checkTests(Event e, PostmanCollectionv2_1 coll, Item it) {
//		if ("text/javascript".equals(e.script.type)) {
//			for (String ex : e.script.exec) {
//				if (ex.contains("pm.expect") || ex.contains("tests[") || ex.contains("pm.test(")) {
//					return "coll " + coll.info.name + (it == null ? "" : " " + it.name) + " forbidden test " + ex;
//				}
//			}
//		}
//		return null;
//	}
//
//	protected static List<String> checkTests(Item it, PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		if (it.event != null) {
//			for (Event e : it.event) {
//				String error = checkTests(e, coll, it);
//				if (error != null) {
//					ret.add(error);
//				}
//			}
//		}
//		if (it.item != null) {
//			for (Item child : it.item) {
//				ret.addAll(checkTests(child, coll));
//			}
//		}
//		return ret;
//	}
//
//	///
//	// check requests that add custom table
//	///
//
//	protected static List<String> checkCTRequests(PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		for (Item it : coll.item) {
//			ret.addAll(checkCTRequests(it, coll));
//		}
//		return ret;
//	}
//
//	@SuppressWarnings("unchecked")
//	protected static List<String> checkCTRequests(Item it, PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		if (it.item != null) {
//			for (Item child : it.item) {
//				ret.addAll(checkCTRequests(child, coll));
//			}
//		}
//		if (it.request != null && it.request.url != null) {
//			if (it.request.url instanceof Map) {
//				Map<String, String> url = (Map<String, String>) it.request.url;
//				if (url.getOrDefault("raw", "").contains("/entityCustomization/entity/createOrUpdate")) {
//					if (it.request.body != null && it.request.body.raw != null) {
//						ret.addAll(checkCTRequest(it.request.body.raw, it, coll));
//					}
//				}
//			}
//		}
//		return ret;
//	}
//
//	private static final ObjectMapper mapper = new ObjectMapper();
//	static {
//		mapper.registerModule(new JaxbAnnotationModule());
//	}
//
//	protected static List<String> checkCTRequest(String body, Item it, PostmanCollectionv2_1 coll) {
//		CustomEntityTemplateDto cet;
//		try {
//			synchronized (mapper) {
//				cet = mapper.readValue(body, CustomEntityTemplateDto.class);
//			}
//		} catch (JsonProcessingException e) {
//			throw new UnsupportedOperationException("catch this", e);
//		}
//		if(cet.getFields()!=null) {
//			List<String>ret = new ArrayList<>();
//			for(CustomFieldTemplateDto f : cet.getFields()) {
//				// forbid list field  with listvalues mapped to empty string
//				if(f.getFieldType()==CustomFieldTypeEnum.LIST) {
//					for( Map.Entry<String, String> e : f.getListValues().entrySet()){
//						if(e.getValue()==null || e.getValue().length()==0){
//							ret.add("coll " + coll.info.name + (it == null ? "" : " " + it.name)
//									+ " forbidden empty value of listvalue for field "
//									+ f.getCode());
//							break;
//						}
//					}
//				}
//				// forbid String fields with maxvalue >4000
//				if(f.getFieldType()==CustomFieldTypeEnum.STRING) {
//					if (f.getMaxValue() != null && f.getMaxValue() > 4000) {
//						ret.add(
//								"coll " + coll.info.name + (it == null ? "" : " " + it.name)
//								+ " forbidden maxvalue>4000 with string for field "
//								+ f.getCode());
//					}
//				}
//			}
//			return ret;
//		}
//		return Collections.emptyList();
//	}
//
//	///
//	// check requests that add custom fields
//	///
//
//	protected static List<String> checkCFRequests(PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		for (Item it : coll.item) {
//			ret.addAll(checkCFRequests(it, coll));
//		}
//		return ret;
//	}
//
//	@SuppressWarnings("unchecked")
//	protected static List<String> checkCFRequests(Item it, PostmanCollectionv2_1 coll) {
//		List<String> ret = new ArrayList<>();
//		if (it.item != null) {
//			for (Item child : it.item) {
//				ret.addAll(checkCFRequests(child, coll));
//			}
//		}
//		if (it.request != null && it.request.url != null) {
//			if (it.request.url instanceof Map) {
//				Map<String, String> url = (Map<String, String>) it.request.url;
//				if (url.getOrDefault("raw", "").contains("/customFieldTemplate/createOrUpdate")) {
//					if (it.request.body != null && it.request.body.raw != null) {
//						ret.addAll(checkCFRequest(it.request.body.raw, it, coll));
//					}
//				}
//			}
//		}
//		return ret;
//	}
//
//	protected static List<String> checkCFRequest(String body, Item it, PostmanCollectionv2_1 coll) {
//		CustomFieldTemplateDto cft;
//		try {
//			synchronized (mapper) {
//				cft = mapper.readValue(body, CustomFieldTemplateDto.class);
//			}
//		} catch (JsonProcessingException e) {
//			throw new UnsupportedOperationException("catch this", e);
//		}
//		List<String> ret = new ArrayList<>();
//		// forbid list field with listvalues mapped to empty string
//		if (cft.getFieldType() == CustomFieldTypeEnum.LIST) {
//			for (Map.Entry<String, String> e : cft.getListValues().entrySet()) {
//				if (e.getValue() == null || e.getValue().length() == 0) {
//					ret.add("coll " + coll.info.name + (it == null ? "" : " " + it.name)
//							+ " forbidden empty value of listvalue for field " + cft.getCode());
//					break;
//				}
//			}
//		}
//		// forbid String fields with maxvalue >4000
//		if (cft.getFieldType() == CustomFieldTypeEnum.STRING) {
//			if (cft.getMaxValue() != null && cft.getMaxValue() > 4000) {
//				ret.add("coll " + coll.info.name + (it == null ? "" : " " + it.name)
//						+ " forbidden maxvalue>4000 with string for field " + cft.getCode());
//			}
//		}
//
//		return ret;
//	}
//
//}
