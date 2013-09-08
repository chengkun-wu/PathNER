package uk.ac.man.PathNER;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tartarus.snowball.ext.englishStemmer;

import uk.ac.man.PathNER.DictionaryMatcher.DictionaryEntry;

import com.wcohen.ss.lookup.SoftTFIDFDictionary;

public class SoftDictionaryMatcher {
	public static int deCnt = 0;
	
	public String curOrgStr = ""; //Current original string
	public List<String> tokens = null;
//	public List<Integer> hotTokenIndex = null;
	public List<List<Integer>> hotDeSetList = null;
	//public Map<String, Set<Integer>> hotDeSetMap = new HashMap<String, Set<Integer>>(); //to enable union of candidate set in the presence of synonyms
	
	public List<List<Integer>> resultList = null;
	public List<MatchSegment> mSegResultList = new ArrayList<MatchSegment>();
	public Map<Integer, Collection<Integer>> tokenDeCandMap = null; 
	
	public static Map<String, Integer> stopwords = new HashMap<String, Integer>();
	public static Map<String, Set<String>> synonymMap = new HashMap<String, Set<String>>();
	public static Map<Integer, DictionaryEntry> dictEntriesMap = new HashMap<Integer, DictionaryEntry>();
	public static Map<String, Integer> dictWordFreq = new HashMap<String, Integer>();
	public static Map<String, String> stemMap = new HashMap<String, String>();
	public static int scoreThreshold = 200;
	public static int maxDistance = 5;
	public static long totalWordFreq = 0;
	public static String separatorStr = " -/()*<>,;.?!\'\"\n:[]";
	
	public enum RunMode {
		 SLOW, FAST, SUPER_FAST
		}
	
	public static RunMode runningMode = RunMode.FAST;
	//public static String separatorStr = " \t\n";
	
	public static englishStemmer stemmer = new englishStemmer();
	
	public boolean externalTokensFlag = false;
	public static SoftTFIDFDictionary dict = null;
	public double softThresholdLow = 0.40;  //Empirically
	public double softThresholdHigh = 0.90;  //Empirically
	public int maxWinLen = 25; //Maximum window length
	public boolean ignoreStopWords = false;
	
	public static String dictFileLoc = "./dict/dict_cmpd.list"; //the dict file needs to end with ".list"
	
	public static int runCnt = 0;
	
	public SoftDictionaryMatcher(){
		try{
			dictInit(dictFileLoc);
			getStopWords();
			buildSynonymMap();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public SoftDictionaryMatcher(RunMode rm){
		this.runningMode = rm;
		
		try{
			dictInit(dictFileLoc);
			getStopWords();
			buildSynonymMap();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public SoftDictionaryMatcher(RunMode rm, String dictFile){
		this.dictFileLoc = dictFile;

		this.runningMode = rm;
		
		try{
			dictInit(dictFileLoc);
			getStopWords();
			buildSynonymMap();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	static public void dictInit(String fileName) throws IOException,ClassNotFoundException
    {
        long start0 = System.currentTimeMillis();
        if (fileName.endsWith(".list")) {
            System.out.println("loading aliases...");
            dict = new SoftTFIDFDictionary();
            dict.loadAliases(new File(fileName));
        } else {
            System.out.println("restoring...");
            dict = SoftTFIDFDictionary.restore(new File(fileName));
        }
        double elapsedSec0 = (System.currentTimeMillis()-start0) / 1000.0;
        System.out.println("loaded in "+elapsedSec0+" sec");
        long start1 = System.currentTimeMillis();
        System.out.println("freezing...");
        dict.freeze();
        double elapsedSec1 = (System.currentTimeMillis()-start1) / 1000.0;
        System.out.println("SecondString initialized");
        //System.out.println("frozen in "+elapsedSec1+" sec");
        //System.out.println("total i/o "+(elapsedSec1+elapsedSec0)+" sec");
    }
	
	public static List<String> getTokens(String str){
		str = str.trim();
		StringTokenizer st = new StringTokenizer(str, separatorStr);
		List<String> wordTokens = new ArrayList<String>();
		
		while(st.hasMoreTokens()) { 
			String key = st.nextToken(); 
			
			wordTokens.add(key);
		}
		
		return wordTokens;
	}
	
	public class MatchResult{
		public String entryName = null;
		public String entryId = null;
		public double score = -1.0;
		
		public MatchResult(String entryName, String entryId, double score){
			this.entryName = entryName;
			this.entryId = entryId;
			this.score = score;
		}
		
		public String toString(){
			StringBuilder sb = new StringBuilder();
			
			sb.append("[").append(entryId).append(":").append(entryName).append("]");
			
			return sb.toString();
		}
	}
	
	public static String getStem(String str){
		
		String stemStr = str;
		
		if(stemMap.get(str)==null){
			stemmer.setCurrent(str);
			stemmer.stem();
			stemStr = stemmer.getCurrent();
			
			stemMap.put(stemStr, str);
		}
		
		return stemStr;
	}
	
	public class MatchSegment{
		public List<Integer> tokenIdxList;
		public List<String> segStrList;
		public List<String> segStrListNoStopWords;
		public String segStr;
		public String pmid;
		public double maxScore;
		public String orgStr;
		public String subOrgStr;
		public int localStartOffset = 0;
		public int localEndOffset = 0;
		public long globalStartOffset = 0;
		public long globalEndOffset = 0;
		public int segStart = 0;
		public int segEnd = 0;
		public List<MatchResult> selectedCands = null;
		
		public MatchSegment(int start, int end, String text, int preStart){
			tokenIdxList = new ArrayList<Integer>();
			segStrList = new ArrayList<String>();
			segStrListNoStopWords = new ArrayList<String>();
			segStr = "";
			
			for(int i = start; i <= end; i++){
				tokenIdxList.add(i);
			}
			
			segStart = start;
			segEnd = end;
			orgStr = text;
			
			getSegStrAndList();
			findOffset(text, preStart);
		}
		
		public void findOffset(String text, int startPoint){
			String orgStrBak = text;
			text = text.toLowerCase();

			Set<Character> sepChars = new HashSet<Character>();
			char[] sepCharArray = separatorStr.toCharArray();

			for(int i = 0; i < sepCharArray.length; i++){
				sepChars.add(sepCharArray[i]);
			}
			
			int start = startPoint, end = startPoint;
			char[] orgChars = text.toCharArray();
			
			if(start < 0)
				start = 0;
			
			do{
				start = text.indexOf(segStrList.get(0), start);
				
				if(start < 0)
					break;
				
				char[] mSegChars = new char[segStr.length()];
				int i = 0;
				int segRealLen = 0;
				
				for(String segToken : segStrList){
					char[] segTokenChars = segToken.toLowerCase().toCharArray();
					
					for(int j = 0; j < segTokenChars.length; j++){
						mSegChars[i++] = segTokenChars[j];
						segRealLen++;
					}
				}
				
				boolean match = false;
				
				int orgOffset = 0;
				int realOffset = 0;
				
				while (realOffset < segRealLen){
					
					while((start+orgOffset) < orgChars.length && sepChars.contains(orgChars[start+orgOffset])){
						orgOffset++;
					}
					
					if((start+orgOffset) >= orgChars.length || realOffset >= segRealLen )
						break;
					
					if(orgChars[start + orgOffset] == mSegChars[realOffset]){
						orgOffset++;
						realOffset++;
						if(realOffset == segRealLen){
							match = true;
							break;
						}
					}else{
						break;
					}
				}
				
				if(match){
					end = start + orgOffset;
				//	System.out.println("(" + start + "," + end + "):" + orgStrBak.substring(start, end));
				//	System.out.println(mSeg.segStr);
					break;
				}
				
				start++;
			}while(start < text.length() && end < text.length());
			
			if(start >= 0){
				localStartOffset = start;
				localEndOffset = end;
				orgStr = text.substring(start,end);
			}else{
				int x = 1;
			}
			
		}
				
		public boolean containToken(String token){
			
			if(segStrList.contains(token) )
				return true;
			
			Set<String> synSet = synonymMap.get(token);
			
			if (synSet != null) {

				for (String syn : synSet) {
					if (segStrList.contains(syn))
						return true;
				}
			}
						
			return false;
		}
		
		public void getSegStrAndList(){
			int segLen = tokenIdxList.size();
			StringBuilder sb = new StringBuilder();
			segStr = "";
			
			if(segLen > 0){
				for(int i = 0; i < segLen; i++){
					String token = tokens.get(tokenIdxList.get(i));
					
					segStrList.add(token);
					
					String stemToken = getStem(token);
					
					if(stopwords.get(token) == null && stopwords.get(stemToken) == null)
						segStrListNoStopWords.add(token);
					
					sb.append(token).append(" ");
				}
			}
			
			segStr = sb.toString().trim();
		}
		
		public String getNonStopwordsStr(){
			int segLen = tokenIdxList.size();
			StringBuilder sb = new StringBuilder();
			
			if(segLen > 0){
				
				int endIndex = segLen - 1;
				
				while(endIndex >= 0){
					String token = tokens.get(tokenIdxList.get(endIndex));
					String stemToken = getStem(token);
					
					if(stopwords.get(token) != null || stopwords.get(stemToken) != null){
						endIndex--;
					}else
						break;
				}
				
				for(int i = 0; i <= endIndex; i++){
					String token = tokens.get(tokenIdxList.get(i));
					String stemToken = getStem(token);
					
					if(stopwords.get(token) == null && stopwords.get(stemToken) == null){
						sb.append(token).append(" ");
					}					
				}
				
				for(int i = endIndex + 1; i < segLen; i++){
					String token = tokens.get(tokenIdxList.get(i));
					sb.append(token).append(" ");
				}
			}
			
			return sb.toString();
		}
		
		public List<MatchResult> getTopScoreCand(List<MatchResult> candidates){
			selectedCands = new ArrayList<MatchResult>();
			
			maxScore = -10000;
			
			for(MatchResult mRes : candidates){
				if(mRes.score > maxScore)
					maxScore = mRes.score;
			}
			
			for(MatchResult mRes : candidates){
				double diff = Math.abs(maxScore - mRes.score);
				
				if( diff < 0.001 && maxScore > softThresholdHigh)
					selectedCands.add(mRes);
			}
			
			return selectedCands;
		}
		
		public List<MatchResult> getTopScoreCand(){

			List<MatchResult> tempList = getCandidates(softThresholdHigh);
			
			if(tempList == null)
				return null;
			
			selectedCands = new ArrayList<MatchResult>();
					
			for(MatchResult mRes : tempList){
				double diff = Math.abs(maxScore - mRes.score);
				
				if( diff < 0.001 && maxScore > softThresholdHigh)
					selectedCands.add(mRes);
			}
			
			return selectedCands;
		}
		
		public List<MatchResult> getCandidates(double threshold){
			selectedCands = null;
			String query = orgStr;
			
			if(ignoreStopWords){
				query = getNonStopwordsStr();
			}
			
			//debug
			query = query.replace("signalling", "signaling");
			
			int result = dict.lookup(threshold, query);
			maxScore = -1000;
			
			for (int i = 0; i < result; i++) {
				String entryName = dict.getResult(i);
				String entryId = (String) dict.getValue(i);
				double score = dict.getScore(i);
				
				if(score > maxScore)
					maxScore = score;
				
				MatchResult mRes = new MatchResult(entryName, entryId, score);
					
				if(selectedCands == null){
					selectedCands = new ArrayList<MatchResult>();
				}
					
				selectedCands.add(mRes);
			}
			
			runCnt++;
			
			return selectedCands;
		}
		
		public void setGlobalOffset(long globalStart){
			globalStartOffset = globalStartOffset + globalStart;
			globalEndOffset = globalEndOffset + globalStart;
		}
		
		public String showMatch(boolean printFlag){
			StringBuilder sb = new StringBuilder();
			
			sb.append(segStr).append("\t").append("[");

			if (this.selectedCands != null) {

				for (int i = 0; i < selectedCands.size(); i++) {
					MatchResult mRes = selectedCands.get(i);

					sb.append("{");

					sb.append(mRes.entryId).append(",");
					sb.append(mRes.entryName).append(",");
					sb.append(mRes.score);

					if (i != selectedCands.size() - 1) {
						sb.append(",");
					}

					sb.append("}");
				}
			}
			
			sb.append("]");
			
			if(printFlag){
				System.out.println(sb.toString());
			}
			
			
			return sb.toString();
		}
	}

	public void getStopWords(){
		String fileName = "dict/pwdict_stopwords.txt";
		
		List<String> stopwordList = uk.ac.man.Utils.FileUtils.file2StrList(fileName);
		
		for(String stopword: stopwordList){
			stopword = stopword.trim();
			stopword = getStem(stopword);
			stopwords.put(stopword, 1);
		}
		
		System.out.println("Stop words retrieved.");
	}
	
	public void buildSynonymMap(){
		String synonymFile = "dict/pwdict_synonym.txt";
		
		List<String> synLines = uk.ac.man.Utils.FileUtils.file2StrList(synonymFile);
		
		for(String line : synLines){
			String[] synonyms = line.split(",");
			
			Set<String> synSet = new HashSet<String>(Arrays.asList(synonyms));
			
			for(String word: synSet){
				synonymMap.put(word, synSet);
			}
		}
		
		System.out.println("Synonym Mapping constructed.");
	}
	
	public List<Integer> getHotTokenIndexList(String orgStr){
		List<Integer> hotTokenIndex = new ArrayList<Integer>();
		int preStart = 0;
		
		for(int i = 0; i < tokens.size(); i++){
			String stemToken = this.getStem(tokens.get(i));
			
			if(stopwords.get(tokens.get(i)) != null || stopwords.get(stemToken) != null){
				continue;
			}
			
			MatchSegment mSeg = new MatchSegment(i, i, this.curOrgStr, preStart);
			
			List<MatchResult> possCandList = mSeg.getCandidates(softThresholdLow);
			
		//	if(possCandList != null && possCandList.size() > 0){
				hotTokenIndex.add(i);
				preStart = mSeg.localEndOffset;
		//	}
		}
		
		return hotTokenIndex;
	}
	
	public boolean containsKeyword(String token){
		token = token.toLowerCase();
		String keywords[] = {"signaling","cascade","transduction","pathway","network","signalling","cascades","pathways", "networks"};
		
		for(String key : keywords){
			if(token.contains(key))
				return true;
		}
		
		return false;
	}
	
	public MatchSegment postCheck(MatchSegment mSeg){
		
		List<MatchResult> newTopCands = new ArrayList<MatchResult>();
		
		if(mSeg.selectedCands == null)
			return mSeg;
		
		for(MatchResult mRes : mSeg.selectedCands){
			boolean checkRes = true;
					
			if(mRes.score < softThresholdHigh)
				checkRes = false;
			
			//If the entry is a single word, then the mention must match perfectly
			if(!mRes.entryName.contains(" ")){
				if(!mSeg.segStr.equalsIgnoreCase(mRes.entryName))
					checkRes = false;
			}
			
			if(containsKeyword(mRes.entryName)){
				if(!containsKeyword(mSeg.segStr))
					checkRes = false;
			}
			
			StringTokenizer resST = new StringTokenizer(mRes.entryName, separatorStr);
			StringTokenizer segST = new StringTokenizer(mSeg.segStr, separatorStr);
			
			int resNameLen = resST.countTokens();
			int segNameLen = segST.countTokens();
			int lenDiff = Math.abs(resNameLen - segNameLen);
			
			if(segNameLen == 1){
				if(!mSeg.segStr.equalsIgnoreCase(mRes.entryName))
					checkRes = false;
			}
			
			if(lenDiff >= 3)
				checkRes = false;
			
			if(resNameLen == 2 && segNameLen == 1)
				checkRes = false;
			
			if(checkRes)
				newTopCands.add(mRes);
		}
		
		if(newTopCands.size() == 0){
			mSeg.selectedCands = null;
		}else
			mSeg.selectedCands = newTopCands;
		
		return mSeg;
	}
	
	public List<MatchSegment> getMatchSegments() {

		List<MatchSegment> matchedSegments = new ArrayList<MatchSegment>();

		if (tokens == null) {
			System.err.println("Please get/set the tokens first!");
			return null;
		}
		
		int preStart = 0;
		int i = 0; 
		int k = 0;
		
		List<Integer> hotTokenIndex = getHotTokenIndexList(this.curOrgStr);

		while( k < hotTokenIndex.size() ){
			i = hotTokenIndex.get(k);
			
			Stack<MatchSegment> tempSegCandStack = new Stack<MatchSegment>(); 
			double maxCandScore = -1;
			
			int allowedMiss = 4; //Still allows expansion if not appropriate matching is found
			int j = 1;
			boolean stopFlag = false;
			
			while(j < maxWinLen && !stopFlag){
				int end = i + j - 1;
				j++;
				
				if((end + 1) > tokens.size()){
					end = tokens.size() - 1;
					stopFlag = true;
				}
				
				MatchSegment mSeg = new MatchSegment(i, end, this.curOrgStr, preStart);
				
				List<MatchResult> tempCandList = null; 
				
				switch (runningMode){
				case SLOW:
					tempCandList = mSeg.getCandidates(softThresholdLow);
					break;
				case FAST:
					tempCandList = mSeg.getCandidates(softThresholdLow);
					break;
				case SUPER_FAST:
					tempCandList = mSeg.getTopScoreCand();
					break;
				default:
					tempCandList = mSeg.getCandidates(softThresholdLow);
					break;
				}
				
				
				if(tempCandList != null && tempCandList.size() > 0){
					tempSegCandStack.push(mSeg);
					
					if(mSeg.maxScore > maxCandScore)
						maxCandScore = mSeg.maxScore;
				}else{
					if(allowedMiss-- <= 0){
						if(runningMode != RunMode.SLOW)
							break;
					}
				}
				
				if(allowedMiss <= 0 && maxCandScore < 0){
					if(runningMode != RunMode.SLOW)
						break;
				}
			}
			
			int nextK = -1;
			
			//Get the longest one: using the Stack
			while (!tempSegCandStack.isEmpty()){
				MatchSegment mSeg = tempSegCandStack.pop();
				double diff = Math.abs(maxCandScore - mSeg.maxScore);
				
				if(diff < 0.001 && maxCandScore > softThresholdHigh){
					MatchSegment checkedSeg = postCheck(mSeg);

					if (checkedSeg.selectedCands != null) {
						matchedSegments.add(checkedSeg);
						preStart = mSeg.localEndOffset;
						int nextI = i + mSeg.tokenIdxList.size();

						while (++nextK < hotTokenIndex.size()) {
							if (hotTokenIndex.get(nextK) >= nextI)
								break;
						}
						
						//Longest candidate retrieved
						break;
					}
				}
			}
			
			//The list version: will lead to duplicates
			/*
			for(MatchSegment mSeg : tempSegCandStack){
				double diff = Math.abs(maxCandScore - mSeg.maxScore);
				
				if(diff < 0.001 && maxCandScore > softThresholdHigh){
					MatchSegment checkedSeg = postCheck(mSeg);

					if (checkedSeg.selectedCands != null) {
						matchedSegments.add(checkedSeg);
						preStart = mSeg.localEndOffset;
						int nextI = i + mSeg.tokenIdxList.size();

						while (++nextK < hotTokenIndex.size()) {
							if (hotTokenIndex.get(nextK) > nextI)
								break;
						}
					}
				}
			}*/
			
			if(nextK < 0)
				k++;
			else
				k = nextK;
		}

		return matchedSegments;
	}
		
	public List<MatchSegment> getMatchSegments(String str){
		//Needs to have a look at @To-DO
		if(str == null || str.trim().length() == 0)
			return null;
		
		List<MatchSegment> resultList = new ArrayList<MatchSegment>();
		
		//In some cases, the string str might contains '\n', need to deal with that.
		str = str.trim();
		String[] strParts = str.split("\n");
		int acLen = 0;

		for(int i = 0; i < strParts.length; i++){
			tokens = getTokens(strParts[i].toLowerCase());
			
			this.curOrgStr = strParts[i];
			
			List<MatchSegment> tempResultList = this.getMatchSegments();
			
			if(tempResultList == null)
				return null;
			
			for(MatchSegment mSeg : tempResultList){
				mSeg.globalStartOffset = mSeg.localStartOffset + acLen;
				mSeg.globalEndOffset = mSeg.localEndOffset + acLen;
				
				resultList.add(mSeg);
			}
			
			acLen += strParts[i].length() + 1;
		}
		
		//There might be duplicates that need to be removed. 
		

		return resultList;
	}

	public String getPmidFromLineStart(String line){
		String pStr = "\\b([0-9]+):";
		Pattern p = Pattern.compile(pStr);
		Matcher m = p.matcher(line);
		String pmid = "";
		
		while(m.find()){
			pmid = m.group(1);
		}
		
		return pmid;
	}
	
	public boolean withDiffSeparator(String s1, String s2){
		
		char[] sepChars = ",;.?!".toCharArray();
		
		for(char sepChar : sepChars){
			
			int i1 = s1.indexOf(sepChar);
			int i2 = s2.indexOf(sepChar);
			
			if(i1 < 0 && i2>=0)
				return true;
			if(i2 <0 && i1 >=0)
				return true;
		}
		
		return false;
	}
		
	public void test(){
		String testFile = "dict_matching_test_50articles.txt";
		//String testFile = "dict_test.txt";
		List<String> strList = uk.ac.man.Utils.FileUtils.file2StrList(testFile);
		int cnt = 0;
		List<String> printList = new ArrayList<String>();
		
		String lastPmid = "";
		
		for(String line: strList){
			
			if(line.trim().length() == 0)
				continue;
			
			lastPmid = getPmidFromLineStart(line);
			
			if(!lastPmid.equals("")){
				line = line.replace(lastPmid + ":", "");
			}
			
			List<MatchSegment> candSegList = new ArrayList<MatchSegment>();
			
			String[] lineSegs = line.split(",!?.:;");
				
			for(String lineSeg : lineSegs){
				
				if(lineSeg.trim().length() > 0)
					candSegList.addAll(this.getMatchSegments(lineSeg));
			}
			
			List<MatchSegment> mSegList = new ArrayList<MatchSegment>();
			mSegList.addAll(candSegList);
					
			for(MatchSegment mSeg : mSegList){
			//	System.out.println(lastPmid);
				
				StringBuilder sb = new StringBuilder();
				sb.append(lastPmid).append('\t');
				sb.append(mSeg.showMatch(false));
				
				printList.add(sb.toString());
				System.out.println(sb.toString());
			}
			
			cnt++;
		//	if(cnt % 1 == 0)
		//		System.out.println("{" + cnt + " results processed}");
			
		}
		
		String outFileName = String.format("softDmResult_%.2f_%.2f_%d_%s.tsv", 
				softThresholdLow, softThresholdHigh, cnt, this.runningMode);
		
		uk.ac.man.Utils.FileUtils.strList2File(printList, outFileName);
	}
	
	
	public void getStatisticsOfMatchedResults(List<MatchSegment> mSegList){
		
		Map<String, Integer> segFreq = new HashMap<String, Integer>();
		Map<String, Set<String>> segPmids = new HashMap<String, Set<String>>();
		Map<String, Integer> segScore = new HashMap<String, Integer>();
		Map<String, String> segDeStr = new HashMap<String, String>();
		
		for (MatchSegment mSeg : mSegList) {
			String segStr = mSeg.segStr;
			Integer freq = segFreq.get(segStr);
			
			if( freq != null){
				freq++;
				Set<String> pmidSet = segPmids.get(segStr);
				pmidSet.add(mSeg.pmid);
				segFreq.put(segStr, freq);
				segPmids.put(segStr,pmidSet);
			}else{
				Set<String> pmidSet = new HashSet<String>();
				pmidSet.add(mSeg.pmid);
				
				segFreq.put(segStr, 1);
				segPmids.put(segStr, pmidSet);
				segScore.put(segStr, (int)mSeg.maxScore);
		//		segDeStr.put(segStr, mSeg.topDe.orgStr);
			}
		}
		
		List<String> outStrList = new ArrayList<String>();
		
		for(String segStr : segFreq.keySet()){
			Integer freq = segFreq.get(segStr);
			Integer score = segScore.get(segStr);
			Set<String> pmidSet = segPmids.get(segStr);
			
			StringBuilder sb = new StringBuilder();
			sb.append(segStr).append('\t');
			sb.append(segDeStr.get(segStr)).append('\t');
			sb.append(freq).append('\t');
			sb.append(score).append('\t');
			
			for(String pmid : pmidSet){
				sb.append(pmid).append(',');
			}
			
			outStrList.add(sb.toString());
		}
		
		uk.ac.man.Utils.FileUtils.strList2File(outStrList, "refined_matched_results_whole_genia.tsv");
	}
		
	public void simpleTest(){
		String text = "Medullary carcinoma of the thyroid misdiagnosed as differentiated thyroid carcinoma.";
		
		List<MatchSegment> mSegList = getMatchSegments(text);
		long start = 0;
		
		for(MatchSegment mSeg : mSegList){
			mSeg.showMatch(true);
			
			start = mSeg.globalStartOffset;
			
			System.out.println("(" + mSeg.globalStartOffset + "," + mSeg.globalEndOffset + ")");
		}
		
	}
	
	public void interativeSimpleTest(){
		this.softThresholdHigh = 0.5;
		
		System.out.println("Running interative simple test....");
		
		Scanner input = new Scanner(new InputStreamReader(System.in));
		
		while (input.hasNext()){
			String text = input.nextLine();
			
			List<MatchSegment> mSegList = getMatchSegments(text);
			long start = 0;
			
			for(MatchSegment mSeg : mSegList){
				mSeg.showMatch(true);
				
				start = mSeg.globalStartOffset;
				
				System.out.println("(" + mSeg.globalStartOffset + "," + mSeg.globalEndOffset + ")");
			}
			
			if(mSegList == null || mSegList.size() == 0){
				System.out.println("No match found!");
			}
		}
	}
	
	public static void main(String[] args){
		//SoftDictionaryMatcher softDm = new SoftDictionaryMatcher();
		SoftDictionaryMatcher softDm = new SoftDictionaryMatcher(RunMode.SUPER_FAST, "tc_dict.list");
					
		long start1 = System.currentTimeMillis();
		
		for(RunMode rm : RunMode.values()){
			
			if (rm != RunMode.SLOW)
				continue;
			
			System.out.println("\n\nRunning in mode : " + rm);
			softDm.runningMode = rm;
			softDm.ignoreStopWords = true;
	//		softDm.softThresholdHigh = 0.8;
	//		softDm.test();
	//		softDm.interativeSimpleTest();
			softDm.simpleTest();
		}
		
		System.out.println(softDm.runCnt + " runs of getCandidates function");
		
		double elapsedSec1 = (System.currentTimeMillis()-start1) / 1000.0;
		 
		System.out.format("Time used: %.3f seconds", elapsedSec1);
	

	}
}
