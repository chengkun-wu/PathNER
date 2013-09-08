package uk.ac.man.PathNER;


import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateConstants;
import gate.ProcessingResource;
import gate.Utils;
import gate.creole.ANNIETransducer;
import gate.creole.ExecutionException;
import gate.creole.POSTagger;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.annotdelete.AnnotationDeletePR;
import gate.creole.splitter.RegexSentenceSplitter;
import gate.creole.splitter.SentenceSplitter;
import gate.creole.tokeniser.DefaultTokeniser;
import gate.util.AnnotationDiffer;
import gate.util.InvalidOffsetException;
import gate.util.LuckyException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;

import uk.ac.man.PathNER.SoftDictionaryMatcher.MatchResult;
import uk.ac.man.PathNER.SoftDictionaryMatcher.MatchSegment;
import uk.ac.man.PathNER.SoftDictionaryMatcher.RunMode;
import uk.ac.man.Utils.ArgParser;
import uk.ac.man.Utils.DocGeneFilter;
import banner.tagging.Mention;


/*
 * Author: Chengkun Wu
 */
public class PathNER {
	public class MergedMatch{
		long startOffset;
		long endOffset;
		String text;
		String matchedStr;
		String entry;
	}

	public static boolean sentence_sep = true;
	public static DocGeneFilter gf = null;
	public static SoftDictionaryMatcher dm = null;
	
	private static DefaultTokeniser tokeniser = null;
	private static SentenceSplitter splitter = null;
	private static RegexSentenceSplitter regexSplitter = null;
	private static POSTagger tagger = null;
	private static AnnotationDeletePR pr = null;
	private static ANNIETransducer jape = null;

	private static SerialAnalyserController geniaPipeline = null;
	private static SerialAnalyserController anniePipeline = null;
	private static SerialAnalyserController opennlpPipeline = null;
	private static SerialAnalyserController lingPipePipeline = null;
	public static int tagComponentCount = 2; //Currently rule-based and soft dictionary
	
	public boolean writeGateDoc = false;
	
	//To-Do
	//private static File pluginsDir = Gate.getPluginsHome();
	private static File pluginsDir = new File("GATE_PLUGINS/");
	
	
	public Map<String, List<String>> goldMap = new HashMap<String, List<String>>();
	public Map<String, List<String>> annotatedMap = new HashMap<String, List<String>>();
	public List<String> pwMentionStrList = new ArrayList<String>();
	
	public int reportNumber = 10;
	
	//Feature related
	public enum FEATURE_TYPE {TOKEN_STR, POS_TAG, GENE, IS_KEYWORD};
	
 	public void initPipelines() throws ResourceInstantiationException {
		
 		
		anniePipeline  = (SerialAnalyserController) Factory
		.createResource("gate.creole.SerialAnalyserController", Factory
				.newFeatureMap(), Factory.newFeatureMap(), "ANNIE_"
				+ Gate.genSym());
		
 		
		opennlpPipeline = (SerialAnalyserController) Factory
		.createResource("gate.creole.SerialAnalyserController", Factory
				.newFeatureMap(), Factory.newFeatureMap(), "GENIA_"
				+ Gate.genSym());
	}
	
	public void initPlugins() throws Exception{
	//  load the plugins
        initANNIE();
        initOpenNLP();
        
        gf = new DocGeneFilter();
        dm = new SoftDictionaryMatcher(RunMode.SUPER_FAST);
        
        
        //Init Jape Engine
        FeatureMap params = Factory.newFeatureMap();
	    final String japeFile = "noun.jape";
	    params.put("grammarURL", (new File(pluginsDir, "noun.jape")).toURI().toURL());
	    jape = (ANNIETransducer) Factory.createResource("gate.creole.ANNIETransducer", params);
	    jape.setOutputASName("pw");
	    
	}
	
	public void initANNIE() throws Exception{
		
		File aPluginDir = new File(pluginsDir, "ANNIE");

		// load the plugin.
		System.out.println(aPluginDir.toURI().toURL());
		Gate.getCreoleRegister()
				.registerDirectories(aPluginDir.toURI().toURL());

		FeatureMap params = Factory.newFeatureMap();
		params.put("keepOriginalMarkupsAS", true);
		pr = (AnnotationDeletePR) Factory.createResource(
				"gate.creole.annotdelete.AnnotationDeletePR", params);

		System.err.println("Loaded: Document Reset PR");

		params = Factory.newFeatureMap();
		tokeniser = (DefaultTokeniser) Factory.createResource(
				"gate.creole.tokeniser.DefaultTokeniser", params);

		// create a splitter
		params = Factory.newFeatureMap();
		splitter = (SentenceSplitter) Factory.createResource(
				"gate.creole.splitter.SentenceSplitter", params);

		// create a pos tagger
		params = Factory.newFeatureMap();
//		tagger = (POSTagger) Factory.createResource("gate.creole.POSTagger",
	//			params);
		
		//Add all the processing resources to the procesing pipeline
		anniePipeline.add(pr);
		anniePipeline.add(tokeniser);
		anniePipeline.add(splitter);
	//	anniePipeline.add(tagger);
	}
	
	public void initOpenNLP() throws Exception{
		File opennlpPluginDir = new File(pluginsDir, "OpenNLP");
		Gate.getCreoleRegister().registerDirectories(opennlpPluginDir.toURI().toURL());
		Gate.getCreoleRegister().registerDirectories((new File(pluginsDir, "Parser_Stanford")).toURI().toURL());
		
		FeatureMap params = Factory.newFeatureMap();
		ProcessingResource openTokeniser = (ProcessingResource) Factory.createResource(
				"gate.opennlp.OpenNlpTokenizer", params);
		
		
		// create a splitter
		params = Factory.newFeatureMap();
		ProcessingResource openSplitter = (ProcessingResource) Factory.createResource(
				"gate.opennlp.OpenNlpSentenceSplit", params);

		// create a pos tagger
		params = Factory.newFeatureMap();
		ProcessingResource openPosTagger = (ProcessingResource) Factory.createResource("gate.opennlp.OpenNlpPOS",
				params);
		
		params = Factory.newFeatureMap();
		ProcessingResource openChunker = (ProcessingResource) Factory.createResource("gate.opennlp.OpenNlpChunker",
				params);

		
		opennlpPipeline.add(openTokeniser);
		opennlpPipeline.add(openSplitter);
		opennlpPipeline.add(openPosTagger);
		opennlpPipeline.add(openChunker);
		//opennlpPipeline.add(stanford);
	}
	
	public void initGate() throws Exception{
		 System.setProperty("file.encoding", "UTF-8");
		
		Gate.runInSandbox(true);

        Gate.init();
        
        initPipelines();
               
        initPlugins();
	}
	
	public Boolean isPW(String str){
		str = str.toLowerCase();
		
		if(str.contains("pathway"))
			return true;
		
		if(str.contains("signalling"))
			return true;
		
		if(str.contains("signaling"))
			return true;
		
		if(str.contains("cascade"))
			return true;
		
		if(str.contains("transduction"))
			return true;
		
		if(str.contains("network")){
			return true;
		}
		
		return false;
	}
	

	public List<Annotation> tagWithOpenNLP(Document doc){
		List<Annotation> as = null;

		try {
			Corpus c = Factory.newCorpus("temp");
			c.add(doc);
			System.out.println(doc.getName() + " being processed by OpenNLP");
			
			opennlpPipeline.setCorpus(c);
			opennlpPipeline.execute();
			c.clear();
			
			
			as = new ArrayList<Annotation>(doc.getAnnotations().get("Token"));
			Collections.sort(as, gate.Utils.OFFSET_COMPARATOR);
			

		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (ResourceInstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return as;
	}

	public void addMatchSegment2AnnotationSet(List<MatchSegment> segList, Document doc){
		if(doc == null){
			System.err.println("Please give valid Gate document!");
			return;
		}
		
		if(segList == null){
			System.err.println("Please give valid list of MatchSegment!");
			return;
		}
			
		AnnotationSet pwAS = doc.getAnnotations("pw");
		
		for(MatchSegment mSeg : segList){
			//Add annotations
			FeatureMap fm = Factory.newFeatureMap();
			fm.put("pwmention", "true");
			fm.put("specific", "true");
			fm.put("method", "sdm");
			MatchResult mRes = mSeg.selectedCands.get(0);
			fm.put("entry", mRes.toString());
			fm.put("score", mRes.score);
			fm.put("string", gate.Utils.stringFor(doc, mSeg.globalStartOffset, mSeg.globalEndOffset));
			
			try {
				//Add the soft dictionary matching (sdm) mention
				pwAS.add(mSeg.globalStartOffset,  mSeg.globalEndOffset, "sdmmention", fm);
				
			} catch (InvalidOffsetException e) {
				throw new LuckyException(e);
			}
		}
	}
	
	public List<Annotation> getMentionsByJapeRules(Document doc){
		List<Annotation> anntList = new ArrayList<Annotation>();
		
		jape.setDocument(doc);
		
		try {
			jape.execute();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		AnnotationSet pwAS = doc.getAnnotations("pw");
		List<Annotation> listAS = new ArrayList<Annotation>(pwAS);
		Collections.sort(listAS,  gate.Utils.OFFSET_COMPARATOR);
		
		if(pwAS == null)
			return null;
		else{
			for(Annotation annt : listAS){
				if(!this.isMentionGood(doc, annt)){
					pwAS.remove(annt);
				//	System.out.println(gate.Utils.stringFor(doc, annt));
				}
			}
		}
		
		//jape.cleanup();
		
		return anntList;
	}
	
	public List<MatchSegment> getMatchSegmentWithDictionaryMatching(Document doc){
		List<MatchSegment> segList = new ArrayList<MatchSegment>();
		
		List<String> lines = new ArrayList<String>();
		List<Annotation> sentAS = new ArrayList<Annotation>();
		
		sentAS.addAll(doc.getAnnotations().get("Sentence"));
		Collections.sort(sentAS, gate.Utils.OFFSET_COMPARATOR);
		
		int count = 0;
		
		for(Annotation sent : sentAS){
			long sentStart = sent.getStartNode().getOffset();
			long sentEnd = sent.getEndNode().getOffset();
			
			String sentStr = gate.Utils.stringFor(doc, sentStart, sentEnd);
					
			List<Annotation> tokenAS = new ArrayList<Annotation>();
			tokenAS.addAll(doc.getAnnotations().get("Token", sentStart, sentEnd));
			Collections.sort(tokenAS, gate.Utils.OFFSET_COMPARATOR);
			
			List<String> tokens = new ArrayList<String>();
								
			List<MatchSegment> sentSegList = dm.getMatchSegments(sentStr);
			
			if(sentSegList == null)
				continue;
			
		//	segList.addAll(sentSegList);
			
			int startOffset = 0;
			
			for (MatchSegment mSeg : sentSegList) {
				// System.out.println(lastPmid);
				
				StringBuilder sb = new StringBuilder();
				//sb.append(pmid).append('\t');
				sb.append(mSeg.segStr).append('\t');
				sb.append(mSeg.selectedCands.get(0).entryName).append('\t');
				sb.append(mSeg.maxScore);
				
				lines.add(sb.toString());
				
				mSeg.setGlobalOffset(sentStart);
								
				segList.add(mSeg);
			}
			
			if(++count%reportNumber == 0){
				System.out.println(count + " sentences processed!");
				//break;
			}
		}
		
	//	System.out.println( segList.size() + " dictionary matches found!");
		
	//	uk.ac.man.Utils.FileUtils.strList2File(lines, "corpus_dictionary_matching_sentence.tsv");
		
		return segList;
	}
	
	//Compare the results generated by the rule-based method and the dictionary matching method using an annotated document;
	public void compareRB2DM(Document doc){
		if(doc == null){
			System.err.println("Document is null!");
			return;
		}
		
		List<Annotation> rbAS = new ArrayList<Annotation>();
		List<Annotation> dmAS = new ArrayList<Annotation>();
		
		AnnotationSet as = doc.getAnnotations("pw").get("pwmention");
		
		for(Annotation annt : as){
			FeatureMap features = annt.getFeatures();

			String method = (String) features.get("method");
			
			if(method.equals("dict")){
				dmAS.add(annt);
			}else
				rbAS.add(annt);
		}
		
		// No annotations
		if (dmAS.size() == 0 && rbAS.size() == 0)
			return;
		
		int rbI = 0, dmI = 0;
		int rbOnly = 0, dmOnly = 0;
		int overLapped = 0;

		
		if (dmAS.size() == 0 || rbAS.size() == 0) {
			rbOnly = rbAS.size();
			dmOnly = dmAS.size();
			overLapped = 0;

			return;
		} else {

			Collections.sort(rbAS, gate.Utils.OFFSET_COMPARATOR);
			Collections.sort(dmAS, gate.Utils.OFFSET_COMPARATOR);

			do {
				long rbStart = rbAS.get(rbI).getStartNode().getOffset();
				long rbEnd = rbAS.get(rbI).getEndNode().getOffset();

				long dmStart = dmAS.get(dmI).getStartNode().getOffset();
				long dmEnd = dmAS.get(dmI).getEndNode().getOffset();

				if (rbStart >= dmEnd) {
					dmI++;
					dmOnly++;
					continue;
				}

				if (dmStart >= rbEnd) {
					rbI++;
					rbOnly++;
					continue;
				}

				String rbStr = gate.Utils.stringFor(doc, rbStart, rbEnd);
				String dmStr = gate.Utils.stringFor(doc, dmStart, dmEnd);

				if (rbStart != dmStart || rbEnd != dmEnd) {
					System.out.println("[RB]: " + rbStr);
					System.out.println("[DM]: " + dmStr);
				}

				if (rbStart < dmEnd) {
					rbI++;
				}

				if (dmStart < rbEnd) {
					dmI++;
				}

				overLapped++;

			} while (rbI < rbAS.size() && dmI < dmAS.size());
		}
		
		System.out.println("DM Only: " + dmOnly);
		System.out.println("RB Only: " + rbOnly);
		System.out.println("Overlapped: " + overLapped);
	}
	
	
	public void mergeRBAndDM(Document doc){
		if(doc == null){
			System.err.println("Document is null!");
			return;
		}
		
		AnnotationSet[] asArray = new AnnotationSet[tagComponentCount];
		asArray[0] = doc.getAnnotations("pw").get("rbmention");
		asArray[1] = doc.getAnnotations("pw").get("sdmmention");

		List<Annotation> rbAS = new ArrayList<Annotation>();
		List<Annotation> dmAS = new ArrayList<Annotation>();

		rbAS.addAll(asArray[0]);
		dmAS.addAll(asArray[1]);
		
		AnnotationSet pwAS = doc.getAnnotations("pw");
		HashMap<Annotation,String> mergeAnns = new HashMap<Annotation, String>();
		
		gate.util.AnnotationMerging.mergeAnnotation(asArray, null, mergeAnns, 1, true);
		
		for(Annotation annt : mergeAnns.keySet()){
			String method = (String) annt.getFeatures().get("method");
			FeatureMap fm = Factory.newFeatureMap();
			fm.put("pwmention", "true");
			fm.put("specific", "true");
			fm.put("method", "merge");
			fm.put("submethod", method);
			
			if(method.equals("rules")){
				AnnotationSet olAS = gate.Utils.getOverlappingAnnotations(asArray[1], annt);
				
				for(Annotation sdmAnnt : olAS){
					long start = sdmAnnt.getStartNode().getOffset();
					long end = sdmAnnt.getEndNode().getOffset();
					fm.put("string", gate.Utils.stringFor(doc,start, end));

					String entry = (String) sdmAnnt.getFeatures().get("entry");
					fm.put("entry", entry);
					
					try {
						pwAS.add(start, end, "mergemention", fm);
					} catch (InvalidOffsetException e) {
						throw new LuckyException(e);
					}
				}
			}else{
				long start = annt.getStartNode().getOffset();
				long end = annt.getEndNode().getOffset();
				fm.put("string", gate.Utils.stringFor(doc,start, end));
				
				if(!method.equals("rules")){
					String entry = (String) annt.getFeatures().get("entry");
					fm.put("entry", entry);
				}
				
				try {
					pwAS.add(start, end, "mergemention", fm);
				} catch (InvalidOffsetException e) {
					throw new LuckyException(e);
				}
			}
		}
		
		//After merge, the original set should be removed;
		//The merged annotations will be named as "mergemention"
		List<Annotation> anntList = new ArrayList<Annotation>(pwAS);
		
		for(Annotation annt : anntList){
			FeatureMap features = annt.getFeatures();

			String method = (String) features.get("method");
			
			if(!method.equals("merge") )
				pwAS.remove(annt);
		}
	}
	
	public Document hybridDetectionOnDoc(Document doc, String outPath){
		
		if (doc == null)
			return null;

		this.tagWithOpenNLP(doc);
			
		System.out.println("Preprocessing");
		
		///Tagging with Rule-based component
		System.out.println("Tagging with rule-based component");
		getMentionsByJapeRules(doc);
		
	
		//Tagging with dictionary matching component
		System.out.println("Tagging with dictionary matching component");
		List<MatchSegment> mSegList = getMatchSegmentWithDictionaryMatching(doc);
		
		if (mSegList != null)
			addMatchSegment2AnnotationSet(mSegList, doc);	
	
		//Merge the tags by two components
		mergeRBAndDM(doc);
		
		if(writeGateDoc){
			String xmlStr = doc.toXml();
			uk.ac.man.Utils.FileUtils.str2File(xmlStr, "gate_temp.xml");
		}
		
		System.out.println("Finished tagging with PathNer!\n");
		
		return doc;
	}
		
	public Document hybridDetectionOnTextStr(String textStr, String docName, String outPath){
		Document doc = null;
		
		try {
			doc = Factory.newDocument(textStr);
			
			doc.setName(docName);
			
			this.hybridDetectionOnDoc(doc, outPath);
			
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		}
		
		return doc;
	}
	
	public Document hybridDetectionOnTxt(String fileName, String outPath){
		File corpus = new File(fileName);
		Document doc = null;
		List<String> resultList = new ArrayList<String>();
		
		try {
			doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", 
			  Utils.featureMap(gate.Document.DOCUMENT_URL_PARAMETER_NAME, corpus.toURI().toURL(),
					  gate.Document.DOCUMENT_ENCODING_PARAMETER_NAME, "UTF-8"));
			
			doc.setName(fileName);
			
			this.hybridDetectionOnDoc(doc, outPath);
			
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return doc;
	}
	

	public boolean isMentionGood(Document d, Annotation annt){
		String menStr = gate.Utils.stringFor(d, annt.getStartNode().getOffset(),
				annt.getEndNode().getOffset());
		
		if(menStr.trim().equals(""))
			return false;
		
		String[] menStrTokens = menStr.split(" ");
		
		for(String token : menStrTokens){
			if(!this.isPW(token)){
				Pattern p = Pattern.compile("[A-Z]|[0-9]");
				Matcher m = p.matcher(token);
				if(m.find())
					return true;
			}
		}
		
		List<Mention> geneMenList = gf.getGeneMentions(menStr);
		//menStr = d.getName() + " " + menStr;

		if (geneMenList != null && geneMenList.size() > 0) {
			return true;
		}

		AnnotationSet sentAS = d.getAnnotations().getCovering("Sentence", gate.Utils.start(annt), gate.Utils.end(annt));
		List<Annotation> sentAnList = new ArrayList<Annotation>(sentAS);
		
		if(sentAS != null && sentAS.size() > 0){
			Annotation sentAnnt = sentAnList.get(0);
			
			String sentStr = gate.Utils.stringFor(d, sentAnnt);
			
			geneMenList = gf.getGeneMentions(sentStr);
			
			if (geneMenList != null && geneMenList.size() > 0) {
				for (Mention geneMen : geneMenList) {
					String geneMenStr = geneMen.getText();

					if (menStr.contains(geneMenStr))
						return true;
				}
			}
		}
		
		return false;
	}
	
	
	public void runPathNerOnGoldCorpus(String fileName){
		File corpus = new File(fileName);
		Document doc = null;
		
		try {
			doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", 
			  Utils.featureMap(gate.Document.DOCUMENT_URL_PARAMETER_NAME, corpus.toURI().toURL(), 
			gate.Document.DOCUMENT_ENCODING_PARAMETER_NAME, "UTF-8"));
			
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} 
		
		//Tagging with PathNer
		this.hybridDetectionOnDoc(doc, "");
		
		AnnotationSet goldAS = doc.getAnnotations("pwgold").get("pwmention");
		AnnotationSet ruleAS = doc.getAnnotations("pw").get("pwmention");
		
		Set<String> features = new HashSet<String>();
		
		features.add("pwmention");
		features.add("specific");
		
		AnnotationDiffer differ = new AnnotationDiffer();
		differ.setSignificantFeaturesSet(features);
		differ.calculateDiff(goldAS, ruleAS);
		
		
		int correct = differ.getCorrectMatches();
		double strictPrecision = differ.getPrecisionStrict();
		double lenientPrecision = differ.getPrecisionLenient();
		double strictRecall = differ.getRecallStrict();
		double lenientRecall = differ.getRecallLenient();
		
		int keys = differ.getKeysCount();
		int reponse = differ.getResponsesCount(); 
		
		int missing = differ.getMissing();
		int partially = differ.getPartiallyCorrectMatches();
		int spurius = differ.getSpurious();
		
		System.out.println("===================");
		System.out.println("Performance Summary");
		System.out.println("===================");
		
		
		System.out.println("[Strict Precision]:\t" + String.format("%.3f", strictPrecision));
		System.out.println("[Lenient Precision]:\t" + String.format("%.3f",lenientPrecision));
		System.out.println("[Strict Recall]:\t" + String.format("%.3f",strictRecall));
		System.out.println("[Lenient Recall]:\t" + String.format("%.3f",lenientRecall));
		
		System.out.println("[#Gold Annotation]:\t" + keys);
		System.out.println("[#PathNer Annotation]:\t" + reponse);
		
		System.out.println("[#True Positives]:\t" + correct);
		System.out.println("[#False Negative]:\t" + missing);
		System.out.println("[#Partial Correct]:\t" + partially);
		System.out.println("[#Spurius]:\t" + spurius);

		String docXml = doc.toXml();
		uk.ac.man.Utils.FileUtils.str2File(docXml, "PathNer_Gold_Test.xml");

		Factory.deleteResource(doc);
	}
	
	public List<MergedMatch> testOnTextStr(String textStr){
		Document doc = this.hybridDetectionOnTextStr(textStr, "textstr_test_doc", "");
		
		AnnotationSet as = doc.getAnnotations("pw").get("pwmention");
		List<MergedMatch> result = new ArrayList<MergedMatch>();
		
		for(Annotation annt : as){
			FeatureMap features = annt.getFeatures();

			String method = (String) features.get("method");
			String string = (String) features.get("string");
			String entry = (String) features.get("entry");
			
			if(method.equals("merge")){
				MergedMatch mMatch = new MergedMatch();
				mMatch.startOffset = annt.getStartNode().getOffset();
				mMatch.endOffset = annt.getEndNode().getOffset();
				mMatch.text = (String) doc.getFeatures().get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);
				mMatch.matchedStr = gate.Utils.stringFor(doc, mMatch.startOffset, mMatch.endOffset);
				mMatch.entry = entry;
			
				StringBuilder sb = new StringBuilder();
				sb.append("[PathNer]: ").append("\t");
				sb.append(mMatch.matchedStr).append("\t");
				
				if(entry != null)
					sb.append(mMatch.entry);
				else
					sb.append("N/A");
				
				System.out.println(sb.toString());
				
				result.add(mMatch);
			}
		}
		
		return result;
	}
	
	public List<MergedMatch> testOnFile(String fileName, String ... outPath){
		
		String outFile = "";
		
		if(outPath.length > 0)
			outFile = outPath[0];
		
		Document doc = this.hybridDetectionOnTxt(fileName, outFile);
		
		AnnotationSet as = doc.getAnnotations("pw").get("mergemention");
		List<MergedMatch> result = new ArrayList<MergedMatch>();
		List<String> printList = new ArrayList<String>();
		
		for(Annotation annt : as){
			FeatureMap features = annt.getFeatures();

			String method = (String) features.get("method");
			String string = (String) features.get("string");
			String entry = (String) features.get("entry");
			
			if(method.equals("merge")){
				MergedMatch mMatch = new MergedMatch();
				mMatch.startOffset = annt.getStartNode().getOffset();
				mMatch.endOffset = annt.getEndNode().getOffset();
				mMatch.text = (String) doc.getFeatures().get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);
				mMatch.matchedStr = gate.Utils.stringFor(doc, mMatch.startOffset, mMatch.endOffset);
				mMatch.entry = entry;
			
				StringBuilder sb = new StringBuilder();
				sb.append("[PathNer]: ").append("\t");
				sb.append(mMatch.matchedStr).append("\t");
				
				if(entry != null)
					sb.append(mMatch.entry);
				else
					sb.append("N/A");
				
				System.out.println(sb.toString());
				
				printList.add(sb.toString());
				result.add(mMatch);
			}
		}
		
		if(!outFile.equals(""))
			uk.ac.man.Utils.FileUtils.strList2File(printList, outFile);
		
		return result;
	}
	
	public static void main(String[] args) throws Exception{
		
		PathNER pathNER = new PathNER();
		pathNER.initGate();
		
		//Handling command line parameters
		ArgParser ap = new ArgParser(args);
		pathNER.reportNumber = ap.getInt("report", 5);
		String goldTestFileName = ap.get("test");
		boolean debug_flag = ap.getBoolean("debug", false);
		
		if(debug_flag)
			pathNER.writeGateDoc = true;

		if(goldTestFileName != null){
			
			if(goldTestFileName.equals("gold")){
				//Default location: 
				goldTestFileName = "./resources/gold_corpus.xml";
				pathNER.runPathNerOnGoldCorpus(goldTestFileName);
			}
			else{
				////File Test
				System.out.println("\n\n***********************************");
				System.out.println("Testing PathNer on a text file");
				System.out.println("***********************************\n\n\n");
				
				String inFile = "";
				
				if(goldTestFileName.equals("test"))
					inFile = "./resources/pathmen_file_test.txt";
				else
					inFile = goldTestFileName;
				
				String outFile = "file_test_result.txt";
				List<MergedMatch> result = pathNER.testOnFile(inFile, outFile);
				
				System.out.println(result.size() + " mentions found!");
				
				System.out.println("\n\n***********************************");
				System.out.println("PathNer text file test completed!");
				System.out.println("***********************************\n\n\n");
			}
			
		}else{
			////Text Str Test
			System.out.println("\n\n***********************************");
			System.out.println("Testing PathNer on a text string");
			System.out.println("***********************************\n\n\n");
			
			String testText = "Delineation of the CD28 signaling cascade was found to involve protein tyrosine kinase activity, followed by the activation of phospholipase A2 and 5-lipoxygenase.";
			List<MergedMatch> result = pathNER.testOnTextStr(testText);
			
			System.out.println(result.size() + " mentions found!");
			
			System.out.println("\n\n***********************************");
			System.out.println("Text string test completed!");
			System.out.println("***********************************\n\n\n");
		}
		
	}
}
