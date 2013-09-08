package uk.ac.man.PathNER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Class which is used to clean and convert ConsensusPathDB data into a "ID	EntryName" format.
 * There are cases where weird characters starting with "&#"; greek letters written in "&alpha;" form;
 * 
 * @author Chengkun Wu
 */

public class DictCPDB {
	private String dbFile = "CPDB_pathways_genes.tab";
	private Map<String, Integer> dsStat = new HashMap<String, Integer>();
	
	public void readDbFile(){
		List<String> lines = uk.ac.man.Utils.FileUtils.file2StrList(dbFile);
		List<String> newLines = new ArrayList<String>();
		
		int cnt = 0;
		int maxTokenCnt = 0;
		
		for(String line : lines){
			if(cnt == 0){
				cnt++;
				continue;
			}
			
			cnt++;
			
			String[] fields = line.split("\t");
			
			if(fields.length != 3){
				System.err.println(line);
			}
			
			String dsName = fields[1]; //Data source name;
			Integer dsId = dsStat.get(dsName); 
			
			if(dsId == null){
				dsId = 1;
			}else{
				dsId++;
			}
				
			dsStat.put(dsName, dsId); //Update the latest ID

			String entryId = dsName + ":" + dsId;
			
			
			String entryName = fields[0];
			
			if(dsName.equals("KEGG")){
				entryName = entryName.replace(" - Homo sapiens (human)", "");
			}
			
			if(entryName.contains("&#")){
				System.err.println("Weird characters exist. Please check");
				System.exit(1);
			}
			
			StringBuilder sb = new StringBuilder();
			
			sb.append(entryId).append("\t").append(entryName);
			
			StringTokenizer st = new StringTokenizer(entryName);
			
			if(st.countTokens() > maxTokenCnt){
				maxTokenCnt = st.countTokens();
			}
			
			newLines.add(sb.toString());
		}
		
		System.out.println(cnt + " lines read in.");
		System.out.println("Maximum token number in a line: " + maxTokenCnt);
		
		uk.ac.man.Utils.FileUtils.strList2File(newLines, "dict_cmpd.list");
	}
	
	public static void main(String[] args){
		DictCPDB dictCPDB = new DictCPDB();
		
		dictCPDB.readDbFile();
	}
}
