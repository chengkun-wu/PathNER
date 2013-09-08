package uk.ac.man.PathNER;

import java.util.ArrayList;
import java.util.List;

public class Article {

	public String id_ext;
	public String source;
	public String xml;
	public String id_map; // Only used for PMC articles
	public String text_title;
	public String text_abstract;
	public String text_body;
	public String journal;
	public String date;
	public String affiliation;
	public List<String> authors;
	public List<String> pubTypeList;
	public List<String> mesh_terms;

	public Article(String id_ext, String source) {
		this.id_ext = id_ext;
		this.source = source;

		this.xml = "";
		this.id_map = "";
		this.text_title = "";
		this.text_abstract = "";
		this.text_body = "";
		this.journal = "";
		this.date = "";
		this.affiliation = "";
		this.authors = new ArrayList<String>();
		this.pubTypeList = new ArrayList<String>();
		this.mesh_terms = new ArrayList<String>();
	}
	
	@Override
	public String toString() {
		String str = "Article [id_ext=" + id_ext + ",\n source=" + source
				+ ",\n xml=" + xml + ",\n id_map=" + id_map + ",\n text_title="
				+ text_title + ",\n text_abstract=" + text_abstract
				+ ",\n text_body=" + text_body + ",\n journal=" + journal
				+ ",\n date=" + date;

		str += ",\n authors = ";
		for (String author : authors)
			str += author + ",";

		str += "\n pubTypeList = ";
		for (String pubtype : pubTypeList)
			str += pubtype + ",";

		str += "\n mesh terms = ";
		for (String mesh : mesh_terms)
			str += mesh + ",";

		return str;
	}
	
	public String list2String(List<String> listObj){
		String str = "";
		
		for (String obj: listObj){
			str += (String) obj + "|";
		}
		
		if (!str.equals(""))
			str = str.substring(0, str.length()-1);
		
		return str;
	}
	
	public String getTextAbastract(){
		return text_title + "\n" + text_abstract;
	}
	
	public String getFullText(){
		return text_title + "\n" + text_abstract + "\n" + text_body;
	}
	
}
