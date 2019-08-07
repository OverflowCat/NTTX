package io.kurumi.ntt.utils;

import cn.hutool.core.util.StrUtil;
import java.math.BigInteger;
import cn.hutool.core.util.ArrayUtil;

/**
 **
 **    零宽度字符水印
 **    
 **    https://www.freebuf.com/articles/web/167903.html
 **
 **/

public class ZeroPad {

	public static String decodeFrom(String text) {

		String encoded = "";

		for (char c : text.substring(1).toCharArray()) {

			if (c == 8203) {

				encoded += "0";

			} else if (c == 8204) {

				encoded += "1";

			}

		}
		
		if (encoded.length() == 0) return "";
		
		if (encoded.startsWith("0")) {
			
			encoded = encoded.substring(1);
			
		} else {
			
			encoded = "-" + encoded.substring(1);
			
		}

		return StrUtil.utf8Str(new BigInteger(encoded,2).toByteArray());

	}

	public static String encodeTo(String text,String content) {

		text = text.replace((char)8203 + "","").replace((char)8204 + "","");

		String encoded = new BigInteger(StrUtil.utf8Bytes(content)) .toString(2);

		if (!encoded.startsWith("-")) {

			encoded = "0" + encoded;

		} else {

			encoded = "1" + encoded.substring(1);

		}

		content = "";

		for (char str : encoded.toCharArray()) {

			if (str == '0') {

				content += (char)8203;

			} else {

				content += (char)8204;

			}

		}

		int length;

		if ((length = text.length()) < 2) {

			return content + text;

		}
		
		String[] encodedArray = StrUtil.split(encoded,length - 1);

		StringBuilder result = new StringBuilder();
		
		int index = 0;
		
		for (;index < encodedArray.length;index ++) {
			
			result.append(text.substring(index,index + 1));
			
			result.append(encodedArray[0]);
			
		}
		
		if (index < length) {
			
			result.append(text.substring(index,length));
			
		}
		
		return result.toString();

	}

}