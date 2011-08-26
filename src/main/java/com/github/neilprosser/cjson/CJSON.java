package com.github.neilprosser.cjson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Helper class providing the {@link #pack(String)} and {@link #unpack(String)} methods.
 * <p>
 * The CJSON format is described in <a href="http://stevehanov.ca/blog/index.php?id=104">Steve Hanov's Blog</a>.
 */
public class CJSON {
	
	private static final String CJSON = "cjson";
	private static final String FORMAT = "f";
	private static final String TEMPLATES = "t";
	private static final String VALUES = "v";
	private static final String EMPTY = "";
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private CJSON() {
	}

	/**
	 * Packs the given JSON string into CJSON.
	 * 
	 * If the given string does not represent a JSON object or array it will be returned untouched.
	 * 
	 * @param json The JSON string which will be packed
	 * @return The CJSON string
	 */
	public static String pack(String json) {
		
		try {
			JsonNode in = MAPPER.readTree(json);
			JsonNode out = compress(in);
			
			if (out == null) {
				return json;
			} else {
				return MAPPER.writeValueAsString(out);
			}
		} catch (IOException e) {
			throw new CJSONException("Couldn't read JSON.", e);
		}
		
	}
	
	/**
	 * Unpacks the given CJSON string into JSON.
	 * 
	 * If the given string is not a CJSON string it will be returned untouched.
	 * 
	 * @param cjson The CJSON string which will be unpacked
	 * @return The JSON string
	 */
	public static String unpack(String cjson) {
		
		try {
			JsonNode in = MAPPER.readTree(cjson);
			
			if (!in.isObject() || !CJSON.equals(in.path(FORMAT).getValueAsText())) {
				return cjson;
			}
			
			JsonNode out = expand(in.get(TEMPLATES), in.get(VALUES));
			return MAPPER.writeValueAsString(out);
				
		} catch (IOException e) {
			throw new CJSONException("Couldn't read JSON.", e);
		}
		
	}
	
	private static JsonNode compress(JsonNode in) {
		Node root = new Node(null, EMPTY);
		
		JsonNode values = process(root, in);
		ArrayNode templates = createTemplates(root);
		
		if (templates.size() == 0) {
			return null;
		}
		
		ObjectNode out = MAPPER.createObjectNode();
		out.put(FORMAT, CJSON);
		out.put(TEMPLATES, templates);
		out.put(VALUES, values);
		
		return out;
	}
	
	private static JsonNode process(Node root, JsonNode target) {
		
		if (target.isArray()) {
			
			ArrayNode result = MAPPER.createArrayNode();
			
			Iterator<JsonNode> elements = target.getElements();
			
			while (elements.hasNext()) {
				JsonNode element = elements.next();
				result.add(process(root, element));
			}
			
			return result;
			
		} else if (target.isObject()) {
			
			Node node = root;
			
			ObjectNode result = MAPPER.createObjectNode();
			ArrayNode values = MAPPER.createArrayNode();
			result.put("", values);
			
			Iterator<Entry<String, JsonNode>> fields = target.getFields();
			
			while (fields.hasNext()) {
				Entry<String, JsonNode> field = fields.next();
				String key = field.getKey();
				JsonNode currentNode = field.getValue();
				node = node.follow(key);
				values.add(process(root, currentNode));
			}
			
			node.links.add(result);
			
			return result;
			
		} else {
			return target;
		}
		
	}
	
	private static ArrayNode createTemplates(Node root) {
		
		Queue<Node> queue = new LinkedList<Node>();
		
		root.templateIndex = 0;
		
		for (Node child : root.children.values()) {
			queue.add(child);
		}
		
		ArrayNode templates = MAPPER.createArrayNode();
		
		while (!queue.isEmpty()) {
			Node node = queue.remove();
			int numberOfChildren = 0;
			
			for (Node child : node.children.values()) {
				queue.add(child);
				numberOfChildren++;
			}
			
			if (numberOfChildren > 1 || node.links.size() > 0) {
				ArrayNode template = MAPPER.createArrayNode();
				Node current = node;
				
				while (current.templateIndex == null) {
					template.insert(0, current.key);
					current = current.parent;
				}
				
				template.insert(0, current.templateIndex);
				
				templates.add(template);
				node.templateIndex = templates.size();
				
				for (int index = 0; index < node.links.size(); index++) {
					((ArrayNode) ((ObjectNode) node.links.get(index)).get(EMPTY)).insert(0, node.templateIndex);
				}
				
			}
			
		}
		
		return templates;
		
	}
	
	private static JsonNode expand(JsonNode templates, JsonNode value) {
		
		if (value.isArray()) {
			
			ArrayNode result = MAPPER.createArrayNode();
			
			Iterator<JsonNode> elements = value.getElements();
			
			while (elements.hasNext()) {
				result.add(expand(templates, elements.next()));
			}
			
			return result;
			
		} else if (value.isObject()) {
			
			ObjectNode result = MAPPER.createObjectNode();
			
			ArrayNode values = (ArrayNode) value.get(EMPTY);
			
			if (values.size() != 0) {
				List<String> keys = getKeys(templates, values.get(0).getValueAsInt());
				
				for (int index = 0; index < keys.size(); index++) {
					result.put(keys.get(index), expand(templates, values.get(index + 1)));
				}
			}
			
			return result;
			
		} else {
			return value;
		}
		
	}
	
	private static List<String> getKeys(JsonNode templates, int index) {
		
		List<String> keys = new ArrayList<String>();
		
		while (index > 0) {
			Iterator<JsonNode> elements = templates.get(index - 1).getElements();
			
			List<String> thisLot = new ArrayList<String>();
			
			while (elements.hasNext()) {
				JsonNode element = elements.next();
				thisLot.add(element.getValueAsText());
			}
			
			thisLot.remove(0);
			keys.addAll(0, thisLot);
			index = templates.get(index - 1).get(0).getValueAsInt();
			
		}
		
		return keys;
		
	}
	
	private static class Node {
		final Node parent;
		final String key;
		final Map<String, Node> children;
		Integer templateIndex;
		final ArrayNode links;
		
		private Node(Node parent, String key) {
			this.parent = parent;
			this.key = key;
			this.children = new LinkedHashMap<String, Node>();
			this.templateIndex = null;
			this.links = MAPPER.createArrayNode();
		}
		
		private Node follow(String key) {
			if (!children.containsKey(key)) {
				children.put(key, new Node(this, key));
			}
			
			return children.get(key);
		}
		
	}

}
