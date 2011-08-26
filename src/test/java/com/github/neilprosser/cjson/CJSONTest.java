package com.github.neilprosser.cjson;

import static com.github.neilprosser.cjson.CJSON.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class CJSONTest {
	
	@Test
	public void packingEmptyObjectIsJustReturned() throws Exception {
		assertThat(pack("{}"), is("{}"));
	}
	
	@Test
	public void packingEmptyArrayIsJustReturned() throws Exception {
		assertThat(pack("[]"), is("[]"));
	}
	
	@Test
	public void unpackingNonCJSONObjectIsJustReturned() throws Exception {
		assertThat(unpack("{\"hello\":\"world\"}"), is("{\"hello\":\"world\"}"));
	}
	
	@Test
	public void simplePackTest() throws Exception {
		
		String test = "{\"key\":\"value\"}";
		String expected = "{\"f\":\"cjson\",\"t\":[[0,\"key\"]],\"v\":{\"\":[1,\"value\"]}}";
		
		String result = pack(test);
		
		assertThat(result, is(expected));
		
	}
	
	@Test
	public void simpleUnpackTest() throws Exception {
		
		String test = "{\"f\":\"cjson\",\"t\":[[0,\"key\"]],\"v\":{\"\":[1,\"value\"]}}";
		String expected = "{\"key\":\"value\"}";
		
		String result = unpack(test);
		
		assertThat(result, is(expected));
		
	}
	
	@Test
	public void jenkinsTest() throws Exception {
		
		String test = fromFile("jenkins.json");
		
		String packed = pack(test);
		
		assertThat(packed, is(fromFile("jenkinsafter.json")));
		
		String result = unpack(packed);
		
		assertThat(result, is(test));
		
	}
	
	public static String fromFile(String path) {
		try {
			return IOUtils.toString(CJSONTest.class.getClassLoader().getResourceAsStream(path), "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
