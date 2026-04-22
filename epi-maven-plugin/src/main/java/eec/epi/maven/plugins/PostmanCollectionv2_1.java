package eec.epi.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import eec.epi.maven.plugins.PostmanCollectionv2_1.Item.Request.Header;

/**
 * see https://schema.getpostman.com/json/collection/v2.1.0/collection.json
 *
 */
public class PostmanCollectionv2_1 {

	public static PostmanCollectionv2_1 loadCollectionFile(File file) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(file, PostmanCollectionv2_1.class);
		} catch (IOException e) {
			throw new UnsupportedOperationException("for file " + file.getAbsolutePath(), e);
		}
	}

	public void write(File file) {
		ObjectMapper mapper = new ObjectMapper();
		// ignore the null fields globally
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		// use printer to add newlines and indent
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
		try {
			file.getParentFile().mkdirs();
			mapper.writer(printer).writeValue(file, this);
		} catch (IOException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	public Auth auth;
	public List<Event> event;
	public Info info = new Info();
	public List<Item> item = new ArrayList<>();
	public Object protocolProfileBehavior;
	public Object variable;

	public static class Auth {
		public String type;
		public List<Header> apikey, awsv4, basic, bearer, digest, edgegrid, hawk, noauth, oauth1, oauth2, ntlm;
	}

	public static class Event {
		public boolean disabled = false;
		public String id;
		public String listen;
		public Script script = new Script();
	}

	public static class Script {
		public String id;
		public String type;
		public ArrayList<String> exec = new ArrayList<>();
		public Object src;
		public String name;
	}

	public static class Info {
		public String _collection_link;
		public String _exporter_id;
		public String _postman_id;
		public String description;
		public String name;
		public String schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";
		public String version;
	}

	public static class Item {
		public Auth auth;
		public String description;
		public List<Event> event;
		public String id;
		public List<Item> item;
		public String name;
		public Object protocolProfileBehavior;
		public Request request;
		public Object response;
		public Object variable;

		public static class Request {
			public Auth auth;
			public Body body;
			public Object certificate;
			public String description;
			// TODO is oneOf, so can be a single string.
			public List<Header> header;
			public String method;
			public Object proxy;
			public Object url;

			public static class Body {
				public Boolean disabled;
				public Object file;
				public List<Object> formdata;
				public Object graphql;
				public String mode;
				public Object options;
				public String raw;
				public List<Object> urlencoded;
			}

			public static class Header {
				public String description;
				public Boolean disabled;
				public String key;

				// not in scheme but present in postman collections
				public String name;

				// not in scheme but present in postman collections
				public String type;
				public String value;

				// not in scheme but present in postman collections
				public String warning;

				public static Header keyVal(String key, String value) {
					Header ret = new Header();
					ret.type = "string";
					ret.key = key;
					ret.value = value;
					return ret;
				}
			}
		}

		public static Item item(String name) {
			Item ret = new Item();
			ret.name = name;
			return ret;
		}

		public static Item itemGoup(String name) {
			Item ret = new Item();
			ret.name = name;
			ret.item = new ArrayList<>();
			return ret;
		}

	}

	public Item toItem() {
		Item newItem = new Item();
		newItem.auth = auth;
		newItem.event = event;
		newItem.item = item;
		newItem.name = info.name;
		return newItem;
	}

	public static final String METANAME = ".META";

	protected Item metaData() {
		if (item != null && !item.isEmpty()) {
			Item item0 = item.get(0);
			if (METANAME.equals(item0.name) && item0.item != null) {
				return item0;
			}
		}
		Item meta = Item.itemGoup(METANAME);
		item.add(0, meta);
		return meta;
	}

	protected void addMetaData(Item meta, String key, String value) {
		Item opt = meta.item.stream().filter(it -> it.name.startsWith(key + "=")).findFirst().orElse(null);
		if (opt == null) {
			opt = Item.itemGoup(key);
			meta.item.add(opt);
		}
		opt.name = key + "=" + value;
	}

	public void addMetaData(String version, String release) {
		Item meta = metaData();
		addMetaData(meta, "version", version);
		addMetaData(meta, "release", release);
	}

	/**
	 *
	 * @param listen
	 *          should be either "prerequest" or "test" according to posman doc
	 * @param type
	 *          type of script to use
	 * @param exec
	 *          script to add.
	 */
	public void addScript(String listen, String type, String exec) {
		if (event == null) {
			event = new ArrayList<>();
		}
		Event foundTest = null;
		for (Event e : event) {
			if (listen.equals(e.listen) && e.script != null && type.equals(e.script.type)) {
				foundTest = e;
			}

		}
		if (foundTest == null) {
			foundTest = new Event();
			foundTest.listen = listen;
			foundTest.script.type = type;
			event.add(foundTest);
		}
		foundTest.script.exec.add(exec);
	}

	public static final String JAVASCRIPT_TYPE = "text/javascript";
	public static final String TEST_LISTEN = "test";

	public void addJavaScriptTest(String jsScript) {
		addScript(TEST_LISTEN, JAVASCRIPT_TYPE, jsScript);
	}

	// @formatter:off
	public static final String JAVASCRIPT_RESPONSETEST=
			"if ([401, 504].includes(pm.response.code)) {\n"
					+ "    // in case of some error , we need to wait 5s and start again\n"
					+ "    console.warn(\"resending same request after 5s\")\n"
					+ "    var endTime = new Date().getTime() + 5000;\n"
					+ "    while (new Date().getTime() < endTime) { }\n"
					+ "    postman.setNextRequest(pm.info.requestId);\n"
					+ "} else {\n"
					+ "    pm.test(\"success reponse code\", () => {\n"
					+ "        pm.expect(pm.response.code, \"received code \"+pm.response.code+\" with status \"+pm.response.status).to.be.oneOf([200, 201, 202, 204]);\n"
					+ "    });\n"
					+ "}";
	// @formatter:on

	public void addResponseTest() {
		addJavaScriptTest(JAVASCRIPT_RESPONSETEST);
	}
}
