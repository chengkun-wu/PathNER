package uk.ac.man.PathNER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.tartarus.snowball.ext.englishStemmer;

/*
 * Author: Chengkun Wu
 */
public class DictionaryMatcher {
	public static int deCnt = 0;
	
	public String curOrgStr = ""; //Current original string
	public List<String> tokens = null;
	public List<Integer> hotTokenIndex = null;
	public List<List<Integer>> hotDeSetList = null;
	
	public List<List<Integer>> resultList = null;
	public List<MatchSegment> mSegResultList = new ArrayList<MatchSegment>();
	public Map<Integer, Collection<Integer>> tokenDeCandMap = null; 
	
	public static Map<String, Integer> stopwords = new HashMap<String, Integer>();
	public static Map<String, Set<String>> synonymMap = new HashMap<String, Set<String>>();
	public static Map<String, Set<Integer>> dict = new HashMap<String, Set<Integer>>();
	public static Map<Integer, DictionaryEntry> dictEntriesMap = new HashMap<Integer, DictionaryEntry>();
	public static Map<String, Integer> dictWordFreq = new HashMap<String, Integer>();
	public static Map<String, String> stemMap = new HashMap<String, String>();
	public static int scoreThreshold = 200;
	public static int maxDistance = 5;
	public static long totalWordFreq = 0;
	public static String separatorStr = " -/()*<>,;.?!\'\"";
	
	public static englishStemmer stemmer = new englishStemmer();
	
	public boolean externalTokensFlag = false;
	
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
	
	public static List<String> getNonStopwords(String str){
		List<String> wordTokens = getTokens(str.toLowerCase());
		
		List<String> nonStopWords = new ArrayList<String>();
		
		for(String token : wordTokens){
			if(token == null || token.trim().equals(""))
				continue;
			
			String stemToken = getStem(token);
			
			if(stopwords.get(stemToken) != null)
				continue;
			
			nonStopWords.add(token);
		}
		
		return nonStopWords;
	}
	
	public boolean isKeyword(String token){
		token = token.toLowerCase();
		String stemToken = getStem(token);
		String keywords[] = {"signaling","cascade","transduction","pathway","network","signalling","cascades","pathways", "networks"};
		
		for(String key : keywords){
			if(token.contains(key))
				return true;
		}
		
		return false;
	}
	
	public class DictionaryEntry{
		public String orgStr;
		public List<String> features;
		public List<String> orgFeatures;
		public int entryID;
		public int len;
		public long score;
		
		public DictionaryEntry(String entryStr){
			orgStr = entryStr;
			entryID = DictionaryMatcher.deCnt++;
			
			features = getFeatures();
			orgFeatures = getOrgFeatures();
			len = features.size();
		}
			
		public List<String> getFeatures(){
			List<String> candFeatures = DictionaryMatcher.getNonStopwords(orgStr);
			
			return candFeatures;
		}
		
		
		public List<String> getOrgFeatures(){
			List<String> candFeatures = DictionaryMatcher.getTokens(orgStr);
			
			return candFeatures;
		}
		
		public boolean containsToken(String token){
			String stemToken = getStem(token);
			
			for(String feature: orgFeatures){
				String stemFeature = getStem(feature);
				
				if(feature.equalsIgnoreCase(token) ||
						stemFeature.equalsIgnoreCase(stemToken))
					return true;
				
				Set<String> synSet = synonymMap.get(token);

				if (synSet != null) {
					for (String syn : synSet) {
						String stemSyn = getStem(syn);
						if (feature.equals(syn) || stemFeature.equals(stemSyn))
							return true;
					}
				}
			}
			
			
			
			return false;
		}
		
		public boolean isIdentical(List<String> strList){
			
			if(strList == null || features == null)
				return false;
			
			String tempStr = "";
			
			for(String str : strList){
				tempStr += str + " ";
			}
			
			if(tempStr.contains("signal transduction"))
				tempStr = tempStr.replace("signal transduction", "signaling");
			
			Collection<String> nonStopStrSet = new HashSet<String>();
			nonStopStrSet.addAll(getNonStopwords(tempStr));
			
			boolean withKey1 = false;
			boolean withKey2 = false;
			
			Collection<String> intersect = new HashSet<String>();
			
			for(int i = 0; i < features.size(); i++){
				
				String feature = features.get(i);
				String stemFeature = getStem(feature);
				
				withKey1 = withKey1 || isKeyword(feature);
				
				Set<String> synSet = synonymMap.get(feature);

				boolean has = false;
				
				for(String str: nonStopStrSet){
					String stemStr = getStem(str);
					
					withKey2 = withKey2 || isKeyword(str);
					
					if(feature.equalsIgnoreCase(str) ||
							stemFeature.equalsIgnoreCase(stemStr) ||
							(isKeyword(str) && isKeyword(feature))){
						has = true;
						intersect.add(str);
					}
					
					if (synSet != null) {
						for (String syn : synSet) {
							String stemSyn = getStem(syn);
							if (str.equals(syn) || stemStr.equals(stemSyn)){
								has = true;
								intersect.add(str);
							}
							
						}
					}
				}
				
				if(!has)
					return false;
			}
			
			if(!CollectionUtils.isEqualCollection(nonStopStrSet, intersect))
				return false;
			
			boolean key = withKey1 ^ withKey2;
			
			if(key)
				return false;
			
			return true;
		}
		
		
		public boolean containsUpperCase(String str){

			char[] cBuffer = str.toCharArray();
			
			if(cBuffer != null){
				for(int i = 0; i < cBuffer.length; i++){
					if(Character.isUpperCase(cBuffer[i]))
						return true;
				}
			}
			
			return false;
		}
		
		public boolean containsNumber(String str){
			char[] cBuffer = str.toCharArray();
			
			if(cBuffer != null){
				for(int i = 0; i < cBuffer.length; i++){
					if(Character.isDigit(cBuffer[i]))
						return true;
				}
			}
			
			return false;
		}
		
		public boolean containsGreekWord(String str){
			
			String[] commonGreekWords = {"alpha","beta","gamma","delta","epsilon","zeta","eta","theta","iota","kappa","lambda","mu","nu","xi","omicron","pi","rho","sigma","tau","upsilon","phi","chi","psi","omega"};
			
			
			for(int i = 0; i < commonGreekWords.length; i++)
				if(str.equalsIgnoreCase(commonGreekWords[i]))
					return true;
			
			return false;
		}
		
		
		public boolean containsImportantWord(String str){
			
			if(str.endsWith("tion"))
				return true;
			
			if(str.endsWith("ism"))
				return true;
			
			if(str.endsWith("thesis"))
				return true;
			
			if(str.endsWith("tivity"))
				return true;

			return false;
		}
		
		public int morePenalty(String orgStr){
			int penaltyTimes = 2;
			
			if(containsNumber(orgStr) || containsUpperCase(orgStr) || containsGreekWord(orgStr)
					|| containsImportantWord(orgStr))
				penaltyTimes = 20;
			
			return penaltyTimes;
		}
		
		public long getScore(MatchSegment mSeg){
			
			score = 0;
			
			if(features == null)
				return 0;
			
			boolean identicalFlag = false;
			
			if(mSeg.segStr.equalsIgnoreCase(orgStr))
				return 10000;
			
			if(this.isIdentical(mSeg.segStrList))
				identicalFlag = true;
			
			//Penalty for missing keywords
			long penalty = 0;
			boolean noPenFlag = false;
			
			for(String segPart : mSeg.segStrList){
				
				if(isKeyword(segPart)){
					noPenFlag = true;
					break;
				}
			}
			
			for (String token : orgFeatures) {
				
				int speicalTokenPenalty = morePenalty(token);
				
				token = token.toLowerCase();
				String stemToken = getStem(token);
				
				if(stopwords.get(stemToken) != null)
					continue;
				
				Integer freq = dictWordFreq.get(stemToken);
				
				if (freq == null || freq <= 0){
					penalty = totalWordFreq;
					continue;
				}
				
				long itemScore = totalWordFreq / (freq+1);
				
				if(!noPenFlag && isKeyword(token))
					penalty = totalWordFreq*10;
				
				if (mSeg.containToken(token)) {
					score += itemScore;
				} else {
					score -= itemScore*speicalTokenPenalty;
				}
			}
			
			for(String segToken : mSeg.segStrList){
				segToken = segToken.toLowerCase();
				String stemSegToken = getStem(segToken);

				if (stopwords.get(stemSegToken) != null)
					continue;

				Integer freq = dictWordFreq.get(stemSegToken);

				if (freq == null || freq <= 0) {
					penalty += totalWordFreq;
				} else {

					if (!this.containsToken(segToken)) {
						penalty += totalWordFreq / (freq + 1);
					}
				}
			}
			
			score -= penalty;
			
			if(identicalFlag){
				score = 10000;
			}
			
			return score;
		}
		
		public int getRefinedLen(){
			Map<String, Integer> appearMap = new HashMap<String, Integer>();
			int refinedLen = len;
			
			for(String token: features){
				Set<String> synSet = synonymMap.get(token);
				
				if(synSet == null){
					if(appearMap.get(token) == null){
						appearMap.put(token,1);
					}else
						refinedLen--;
				}else{
					boolean refineFlag = false;
					for(String syn : synSet){
					
						if(appearMap.get(syn) == null){
							appearMap.put(syn,1);
						}else{
							refineFlag = true;
							break;
						}
							
					}
					
					if(refineFlag)
						refinedLen--;
				}
			}
			
			return refinedLen;
		}
		
		
	}

	public class MatchSegment{
		public List<Integer> tokenIdxList;
		public List<String> segStrList;
		public List<String> stemSegStrList;
		public List<String> segStrListNoStopWords;
		public String segStr;
		public String pmid;
		public long maxScore;
		public DictionaryEntry topDe = null;
		public String orgStr;
		public int localStartOffset = 0;
		public int localEndOffset = 0;
		public long globalStartOffset = 0;
		public long globalEndOffset = 0;
		public int segStart = 0;
		public int segEnd = 0;
		
		public MatchSegment(int start, int end){
			tokenIdxList = new ArrayList<Integer>();
			segStrList = new ArrayList<String>();
			segStrListNoStopWords = new ArrayList<String>();
			stemSegStrList = new ArrayList<String>();
			segStr = "";
			
			for(int i = start; i <= end; i++){
				tokenIdxList.add(i);
			}
			
			segStart = start;
			segEnd = end;
			
			getSegStrAndList();
		}
		
		public boolean moreThanKeywords(){
			
			for(String token: segStrList){
				
				if(!isKeyword(token))
					return true;
			}
			
			return false;
		}
		
		public boolean containToken(String token){
			String stemToken = getStem(token);
			
			if(segStrList.contains(token) || stemSegStrList.contains(stemToken))
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
			segStr = "";
			
			if(segLen > 0){
				for(int i = 0; i < segLen; i++){
					String token = tokens.get(tokenIdxList.get(i));
					
					segStrList.add(token);
					stemSegStrList.add(getStem(token));
					
					if(stopwords.get(token) == null)
						segStrListNoStopWords.add(token);
					
					segStr += token + " "; 
				}
			}
		}
		
		public DictionaryEntry getTopScoreCand(Collection<Integer> deCand){

			maxScore = -1000000;
			DictionaryEntry tempDE = null;
			
			if(deCand == null){
				return null;
			}
			
		
			for(Integer deID : deCand){
				DictionaryEntry de = dictEntriesMap.get(deID);
				
				long score = de.getScore(this);
				
				if (score > maxScore){
					maxScore = score;
					de.score = score;
					tempDE = de;
				}
			}
					
			topDe = tempDE;
			
			return tempDE;
		}
		
		public void setGlobalOffset(long globalStart){
			globalStartOffset = localStartOffset + globalStart;
			globalEndOffset = localEndOffset + globalStart;
		}
	}
	
	public DictionaryMatcher(){
		System.out.println("\n\n==========================");
		System.out.println("Dictionary initialisation.");
		
		getStopWords();
		buildSynonymMap();
		buildDict();
		
		System.out.println("Dictionary built.");
		System.out.println("==========================\n\n");
	}
	
	public void buildSynonymMap(){
		String synonymFile = "./Dict/pwdict_synonym.txt";
		
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
	
	public void buildDict(){
		String dictFile = "./Dict/pwdict_cpdb.txt";
		
		List<String> dictLines = uk.ac.man.Utils.FileUtils.file2StrList(dictFile);
		
		for(String line: dictLines){
			DictionaryEntry de = new DictionaryEntry(line);
			dictEntriesMap.put(de.entryID, de);
			
			List<String> tokens = getNonStopwords(line.toLowerCase());
			
			for(String token : tokens){
				token = getStem(token);
				
				Set<Integer> deSet = dict.get(token);
				
				if(deSet == null)
					deSet = new HashSet<Integer>();
				
				deSet.add(de.entryID);
				dict.put(token, deSet);
			}
		}
		
		for(String token : synonymMap.keySet()){
		
			Set<String> synonymList = synonymMap.get(token);
			Set<Integer> synUnionDeSet = new HashSet<Integer>();
			
			for(String synonym : synonymList){
				String stemSyn = getStem(synonym);
				
				Set<Integer> deSet = dict.get(stemSyn);
				
				if(deSet != null){
					synUnionDeSet.addAll(deSet);
				}
			}
			
			for(String synonym: synonymList){
				String stemSyn = getStem(synonym);
				
				dict.put(stemSyn, synUnionDeSet);
			}
		}
		
		Collection<DictionaryEntry> deListUnion = new HashSet<DictionaryEntry>();
		
		int cnt = 0;
		
		for(String key: dict.keySet()){
			String stemKey = getStem(key);
			deListUnion = CollectionUtils.union(deListUnion, dict.get(stemKey));
		}

		//Update the frequency of each token
		for(String key: dict.keySet()){
			String stemKey = getStem(key);
			
			if(key.equals("induced"))
				System.out.println(stemKey);
			
			dictWordFreq.put(stemKey, dict.get(stemKey).size());
		}
		//END
		
		//Set total word frequency
		totalWordFreq = dictWordFreq.keySet().size();
		
		/*
		for(Integer freq : dictWordFreq.values()){
			totalWordFreq++;
		}*/
		//END

	}

	public void getStopWords(){
		String fileName = "./Dict/pwdict_stopwords.txt";
		
		List<String> stopwordList = uk.ac.man.Utils.FileUtils.file2StrList(fileName);
		
		for(String stopword: stopwordList){
			stopword = stopword.trim();
			stopword = getStem(stopword);
			stopwords.put(stopword, 1);
		}
		
		System.out.println("Stop words retrieved.");
	}
	
	public List<MatchSegment> getMatchSegments() {

		List<MatchSegment> matchedSegments = new ArrayList<MatchSegment>();

		if (tokens == null) {
			System.err.println("Please get/set the tokens first!");
			return null;
		}

		hotTokenIndex = new ArrayList<Integer>();

		for (int i = 0; i < tokens.size(); i++) {
			String stemToken = getStem(tokens.get(i));
			Set<Integer> deSet = dict.get(stemToken);

			if (deSet != null) {
				hotTokenIndex.add(i);
			}
		}

		int i = 0;
		int preIdx = 0;
		
		if(hotTokenIndex.size() == 0)
			return null;
		
		String stemToken = getStem(tokens.get(hotTokenIndex.get(preIdx)));
		Collection<Integer> preHotDeList = dict.get(stemToken);

		i = 1;
		int lastEnd = -1;
		List<MatchSegment> tempMSegList = new ArrayList<MatchSegment>();
		
		while (i < hotTokenIndex.size()) {
			String t = tokens.get(hotTokenIndex.get(i));
			String t0 = tokens.get(hotTokenIndex.get(i - 1));
			// Collection<Integer> currHotDeList = hotDeSetList.get(i);
			
			if(isKeyword(t)){
				i++;
				
				int segStart = hotTokenIndex.get(preIdx);
				int segEnd = hotTokenIndex.get(i - 1);
				MatchSegment mSeg = new MatchSegment(segStart, segEnd);
				
				DictionaryEntry deCand = mSeg.getTopScoreCand(preHotDeList);
				if(deCand != null)
					tempMSegList.add(mSeg);
				
				if(i == hotTokenIndex.size()){
					if (tempMSegList.size() > 0) {
						int lastIndex = tempMSegList.size() - 1;
						
						while (lastIndex >=0){
							MatchSegment tempMSeg = tempMSegList.get(lastIndex);
							DictionaryEntry tempDeCand = tempMSeg.topDe;
							
							if(tempDeCand != null && tempDeCand.score > scoreThreshold
									&& tempMSeg.moreThanKeywords() && i-1 > lastEnd){
								lastEnd = i-1;
								matchedSegments.add(tempMSeg);
								tempMSegList.clear();
								break;
							}

							lastIndex--;
						}
						
					}
					
				}
				
				continue;
			}
			
			Collection<Integer> currHotDeList = dict.get(getStem(t));
			Collection<Integer> hotListIntersect = null;
			
			if(currHotDeList == null || preHotDeList == null){
				hotListIntersect = null;
			}else
				hotListIntersect = CollectionUtils.intersection(preHotDeList,
					currHotDeList);

			if (hotListIntersect == null
					|| hotListIntersect.size() == 0
					|| (hotTokenIndex.get(i) - hotTokenIndex.get(i - 1) >= maxDistance)) {

				int segStart = hotTokenIndex.get(preIdx);
				int segEnd = hotTokenIndex.get(i - 1);

				MatchSegment mSeg = new MatchSegment(segStart, segEnd);

				DictionaryEntry deCand = mSeg.getTopScoreCand(preHotDeList);
				boolean moreThanKeyword = mSeg.moreThanKeywords();
				

				if (deCand != null ) {
					tempMSegList.add(mSeg);
				}
				
				if (tempMSegList.size() > 0) {
					int lastIndex = tempMSegList.size() - 1;
					
					while (lastIndex >=0){
						MatchSegment tempMSeg = tempMSegList.get(lastIndex);
						DictionaryEntry tempDeCand = tempMSeg.topDe;
						
						if(tempDeCand != null && tempDeCand.score > scoreThreshold
								&& tempMSeg.moreThanKeywords() && segEnd > lastEnd){
							lastEnd = segEnd;
							matchedSegments.add(tempMSeg);
							tempMSegList.clear();
							break;
						}

						lastIndex--;
					}
					
				}

				// reset for the next segment
				preIdx++;
				i = preIdx + 1;

				// Parsing over
				if (preIdx >= hotTokenIndex.size()) {
					break;
				}

				stemToken = getStem(tokens.get(hotTokenIndex.get(preIdx)));
				preHotDeList = dict.get(stemToken);

			} else {
				preHotDeList = hotListIntersect;
				i++;
				
				int segStart = hotTokenIndex.get(preIdx);
				int segEnd = hotTokenIndex.get(i - 1);
				MatchSegment mSeg = new MatchSegment(segStart, segEnd);
				
				DictionaryEntry deCand = mSeg.getTopScoreCand(preHotDeList);
				boolean moreThanKeyword = mSeg.moreThanKeywords();
				
				if (deCand != null ) {

					tempMSegList.add(mSeg);
				}
				
				

				if (i == hotTokenIndex.size()) {
					
					if (tempMSegList.size() > 0) {
						int lastIndex = tempMSegList.size() - 1;
						
						while (lastIndex >=0){
							MatchSegment tempMSeg = tempMSegList.get(lastIndex);
							DictionaryEntry tempDeCand = tempMSeg.topDe;
							
							if(tempDeCand != null && tempDeCand.score > scoreThreshold
									&& tempMSeg.moreThanKeywords() && segEnd > lastEnd){
								lastEnd = segEnd;
								matchedSegments.add(tempMSeg);
								tempMSegList.clear();
								break;
							}

							lastIndex--;
						}
						
					}

					if (hotListIntersect.size() > 0) {
						preIdx++;
						i = preIdx + 1;

						// Parsing over
						if (preIdx >= hotTokenIndex.size()) {
							break;
						}

						stemToken = getStem(tokens.get(hotTokenIndex.get(preIdx)));
						preHotDeList = dict.get(stemToken);
					}

				}
				
			}
		
		}

		return matchedSegments;
	}
	
	public List<MatchSegment> getMatchSegments(String str){
		
		if(str == null || str.trim().length() == 0)
			return null;
		
		tokens = getTokens(str.toLowerCase());
		
		this.curOrgStr = str;
		
		List<MatchSegment> tempResultList = postProcessing(this.getMatchSegments());
		
		if(tempResultList == null)
			return null;
		
		List<MatchSegment> resultList = new ArrayList<MatchSegment>();
		
		int startOffset = 0;
		
		for(MatchSegment mSeg : tempResultList){
			mSeg = this.findOffset(str, mSeg, startOffset);
			resultList.add(mSeg);
			
			startOffset = mSeg.localStartOffset + 1;
		}
		
		return resultList;
	}

	public List<MatchSegment> getMatchSegments(List<String> tokenList){
		
		tokens = new ArrayList<String>();
		
		for(int i = 0; i < tokenList.size(); i++){
			tokens.add(tokenList.get(i));
		}
		
		return this.getMatchSegments();
	}
	
	public Collection<Integer> getDictCandByToken(String token){
		String stemToken = getStem(token);
		Collection<Integer> candCollect = dict.get(stemToken);
		
		Set<String> synSet = synonymMap.get(token);
		
		if(synSet != null){
			Collection<Integer> union = new HashSet<Integer>();
			
			for(String syn : synSet){
				String stemSyn = getStem(syn);
				Collection<Integer> synCandCollect = dict.get(stemSyn);
				
				if(synCandCollect != null){
					union.addAll(synCandCollect);
				}
			}
			
			if(union.size() > 0){
				if(candCollect == null){
					candCollect = new HashSet(union);
				}else
					candCollect.addAll(union);
			}
		}
		
		return candCollect;
	}
	
	public String getPmidFromLineStart(String line){
		String pStr = "\\b([0-9]+):";
		Pattern p = Pattern.compile(pStr);
		Matcher m = p.matcher(line);
		String pmid = "";
		
		while(m.find()){
			pmid = m.group(1);
			
		//	line = line.replace(m.group(0), "");
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
	
	public List<MatchSegment> postProcessing(List<MatchSegment> candList){
		List<MatchSegment> postProcessedList = new ArrayList<MatchSegment>();
		
		if(candList == null)
			return null;
		
		for(MatchSegment mSeg : candList){
			
			//Single word entries should be matched perfectly
			if(mSeg.segStrList.size() == 1){
				String s1 = mSeg.segStr.trim();
				String s2 = mSeg.topDe.orgStr.trim();
				
				if(s1.equalsIgnoreCase(s2)){
					postProcessedList.add(mSeg);
				}
			}else{

				//No extra punctuation should be added. 
				mSeg = this.findOffset(this.curOrgStr, mSeg, 0);
				
				if(withDiffSeparator(mSeg.orgStr, mSeg.topDe.orgStr))
					continue;
				
				if(mSeg.segStrListNoStopWords.size()*2 >= mSeg.topDe.features.size())
					postProcessedList.add(mSeg);
			}
		}
		
		return postProcessedList;
	}
	
	public void test(String outFile){
		String testFile = "./resources/dict_matching_test_50articles.txt";

		List<String> strList = uk.ac.man.Utils.FileUtils.file2StrList(testFile);
		int cnt = 0;
		List<String> printList = new ArrayList<String>();
		String titleLine = "PMID\tDetected Segment\tDict Entry\tScore";
		printList.add(titleLine);
		
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
					candSegList.addAll(postProcessing(this.getMatchSegments(lineSeg)));
			}
			
			List<MatchSegment> mSegList = new ArrayList<MatchSegment>();
			mSegList.addAll(candSegList);
			
			
			for(MatchSegment mSeg : mSegList){
			
				StringBuilder sb = new StringBuilder();
				sb.append(lastPmid).append('\t');
				sb.append(mSeg.segStr).append('\t');
				sb.append(mSeg.topDe.orgStr).append('\t');
				sb.append(mSeg.maxScore);
			
				printList.add(sb.toString());
			}
			
			cnt++;
			if(cnt % 10 == 0)
				System.out.println("{" + cnt + " file lines processed}");
		}
		
		uk.ac.man.Utils.FileUtils.strList2File(printList, outFile);
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
				segDeStr.put(segStr, mSeg.topDe.orgStr);
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
	
	
	public MatchSegment findOffset(String orgStr, MatchSegment mSeg, int startPoint){
		String orgStrBak = orgStr;
		orgStr = orgStr.toLowerCase();

		Set<Character> sepChars = new HashSet<Character>();
		char[] sepCharArray = separatorStr.toCharArray();

		for(int i = 0; i < sepCharArray.length; i++){
			sepChars.add(sepCharArray[i]);
		}
		
		int start = startPoint, end = startPoint;
		char[] orgChars = orgStr.toCharArray();
		
		do{
			start = orgStr.indexOf(mSeg.segStrList.get(0), start);
			
			if(start < 0)
				break;
			
			char[] mSegChars = new char[mSeg.segStr.length()];
			int i = 0;
			int segRealLen = 0;
			
			for(String segToken : mSeg.segStrList){
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
		}while(start < orgStr.length() && end < orgStr.length());
		
		if(start >= 0){
			mSeg.localStartOffset = start;
			mSeg.localEndOffset = end;
			mSeg.orgStr = orgStr.substring(start,end);
		}else{
			int x = 1;
		}
		
		return mSeg;
	}
	
	public static void main(String[] args){
		DictionaryMatcher dm = new DictionaryMatcher();
		//
				
		System.out.println("******************************");
		System.out.println("Testing with DictionaryMatcher");
		System.out.println("******************************\n\n\n");
		
		//Simple test use a string
		System.out.println("Matching test String...");
		String text = "8906805:Lack of IL-12 signaling in human allergen-specific Th2 cells.";
				
		List<MatchSegment> mSegList = dm.postProcessing(dm.getMatchSegments(text));
		int start = 0;
		
		for(MatchSegment mSeg : mSegList){
			System.out.println("[Found mention\t]:" + mSeg.segStr);
			System.out.println("[Dict Entry\t]:" + mSeg.topDe.orgStr);
			System.out.println("[Matching Score\t]:" + mSeg.maxScore);
			
			mSeg = dm.findOffset(text, mSeg, start);
			start = mSeg.localStartOffset;
		}
		
		//Test using a file
		System.out.println("\n\nMatching test file...");
		dm.test("File_Test_Result.txt");
		
		System.out.println("\n\n\n******************************");
		System.out.println("Testing completed!");
		System.out.println("******************************");
		
	}
	
}
