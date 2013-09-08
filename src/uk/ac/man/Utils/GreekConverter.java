package uk.ac.man.Utils;

import java.util.HashMap;
import java.util.Map;

/*
 * @Author: Chengkun Wu
 * Function: Convert the unicode greek symbols in a string to english written symbols
 */
public class GreekConverter {
	public Map<String, String> greekMap = new HashMap<String, String>();
	public String[] uspace = null;

	public GreekConverter(){
		String greekUnicode = "\u03B1|\u03B2|\u03B3|\u0393|\u03DC|\u03B4|\u0394|\u03B5|\u025B|\u03B5|\u03B6|\u03B7|\u03B8|\u0398|\u03D1|\u03B9|\u03BA|\u03F0|\u03BB|\u039B|\u03BC|\u03BD|\u03BE|\u039E|\u03C0|\u03D6|\u03A0|\u03C1|\u03F1|\u03C3|\u03A3|\u03C2|\u03C4|\u03C5|\u03D2|\u03C6|\u03A6|\u03D5|\u03C7|\u03C8|\u03A8|\u03C9|\u03A9";
		String greekConverted = "alpha|beta|gamma|Gamma|digamma|delta|Delta|epsilon|epsilon|epsilon|zeta|eta|theta|Theta|theta|iota|kappa|kappa|lambda|Lambda|mu|nu|xi|Xi|pi|pi|Pi|rho|rho|sigma|Sigma|sigma|tau|upsilon|Upsilon|phi|Phi|phi|chi|psi|Psi|omega|Omega";
		String spaceUnicode = "\u0020|\u00A0|\u1680|\u180E|\u2000|\u2001|\u2002|\u2003|\u2004|\u2005|\u2006|\u2007|\u2008|\u2009|\u200A|\u200B|\u202F|\u205F|\u3000|\uFEFF";
		String spaceConverted = "";
		
		String[]key = greekUnicode.split("\\|");
		String[]value = greekConverted.split("\\|");
		
		uspace = spaceUnicode.split("\\|");
		
		for(int i=0; i<key.length; i++){
			greekMap.put(key[i], value[i]);
		}
	}
	
	
	public String replaceGreekSymbols(String strWithGreek){
		String strWithoutGreek = strWithGreek;
		
		for(String greekUnicode: greekMap.keySet()){
			strWithoutGreek = strWithoutGreek.replaceAll(greekUnicode, greekMap.get(greekUnicode));
		}
		
		return strWithoutGreek;
	}
	
	public String replaceUnicodeSpace(String strWithUnicodeSpace){
		String strWithoutUnicodeSpace = strWithUnicodeSpace;
		
		for(String unicodeSpace: uspace){
			strWithoutUnicodeSpace = strWithoutUnicodeSpace.replaceAll(unicodeSpace, " ");
		}
		
		return strWithoutUnicodeSpace;
	}
	
	public String cleanStringUnicode(String orgStr){
		String str = orgStr;
		
		str = replaceGreekSymbols(str);
		str = replaceUnicodeSpace(str);
		//str = StringEscapeUtils.unescapeJava(str);
		
		return str;
	}
	
	public static void main(String[] args){
		GreekConverter gc = new GreekConverter();
		String orgStr = "nuclear factor-кBp65";
		
		System.out.println(gc.cleanStringUnicode(orgStr));
	}
	
}
