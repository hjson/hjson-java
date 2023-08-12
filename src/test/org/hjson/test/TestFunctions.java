package org.hjson.test;

import org.hjson.JsonValue;
import org.hjson.ParseException;

class TestFunctions {
	public final static int TOO_DEEP_NESTING = 9999;
	public final static String TOO_DEEP_DOC = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ", "0");

	public static String _nestedDoc(int nesting, String open, String close, String content) {
		StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
		for (int i = 0; i < nesting; ++i) {
			sb.append(open);
			if ((i & 31) == 0) {
				sb.append("\n");
			}
		}
		sb.append("\n").append(content).append("\n");
		for (int i = 0; i < nesting; ++i) {
			sb.append(close);
			if ((i & 31) == 0) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

  static void Exec() {
		try {
			JsonValue.valueOf(Math.log(-1)).toString();
			throw new RuntimeException("Accepted NaN double");
		} catch (NumberFormatException e) {
		}

		String jsonString = "[\n[\n=\n[[\'\'\'\'\'\'";
    try {
		  JsonValue.readHjson(jsonString);
			throw new RuntimeException("Accepted non-terminated array");
    } catch (ParseException e) {
    }

		JsonValue.readHjson(_nestedDoc(TOO_DEEP_NESTING, "[ ", "] ", "0"));
		JsonValue.readHjson(_nestedDoc(TOO_DEEP_NESTING, "{ ", "} ", "0"));
		JsonValue.readJSON(_nestedDoc(TOO_DEEP_NESTING, "[ ", "] ", "0"));
		JsonValue.readJSON(_nestedDoc(TOO_DEEP_NESTING, "{ ", "} ", "0"));
  }
}
