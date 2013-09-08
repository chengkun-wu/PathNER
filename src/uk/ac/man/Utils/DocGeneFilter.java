package uk.ac.man.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import uk.ac.man.PathNER.Article;
import banner.BannerProperties;
import banner.Sentence;
import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;

/*
 * Author: Chengkun Wu
 */

public class DocGeneFilter {
	
	Tokenizer tokenizer = null;
    CRFTagger tagger = null;
    PostProcessor postProcessor = null;
    
    public static GreekConverter gc = new GreekConverter();
    public static Logger logger = Logger.getLogger(DocGeneFilter.class.getName());
    
	List<Mention> geneMentions;
	
	public DocGeneFilter(){
		logger.info("Initialising BANNER Gene tagger...");
		if (loadGeneTagger()){
			logger.info("BANNER tagger successfully loaded...");
		}
	}
	
	public DocGeneFilter(Tokenizer paraTokenizer, 
			CRFTagger paraTagger, PostProcessor paraPostProcessor){
		tokenizer = paraTokenizer;
		tagger = paraTagger;
		postProcessor = paraPostProcessor;
	}
	
	public boolean loadGeneTagger(){
		String propertiesFilename = "BANNER/banner.properties"; // banner.properties
        String modelFilename = "BANNER/gene.bin"; // model.bin
        String inputFilename = "test.txt";
        String outputFilename = "BANNER/banner.out";

        // Get the properties and create the tagger
        try {
        	BannerProperties properties = BannerProperties.load(propertiesFilename);
	        tokenizer = properties.getTokenizer();
			tagger = CRFTagger.load(new File(modelFilename), properties.getLemmatiser(), properties.getPosTagger());
			postProcessor = properties.getPostProcessor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Error in loading Gene Tagger");
			e.printStackTrace();
		}
        
		if (tokenizer == null || tagger == null || postProcessor == null)
			return false;
		
		return true;
	}
	
	public List<Mention> getGeneMentions(String docText) {
		geneMentions = null;

		if (tokenizer == null || tagger == null || postProcessor == null
				|| docText == null || docText.length() ==0) {
			System.out.println("BANNER components not set properly");
			return null;
		}

		String sentenceText = docText;
		if (sentenceText.length() > 0) {
			try{
				Sentence sentence = new Sentence(null, sentenceText);
				tokenizer.tokenize(sentence);
				tagger.tag(sentence);
				if (postProcessor != null)
					postProcessor.postProcess(sentence);

				geneMentions = sentence.getMentions();
			} catch (Exception e){
				return null;
			}
		}
		
		return geneMentions;
	}
	
	public Map<String, List<Mention>> getGeneMentions(Map<String, Article> mapArticles){
		Map<String, List<Mention>> mapArtListGenes = new HashMap<String, List<Mention>>();
		
		for(String pmid: mapArticles.keySet()){
			Article article = mapArticles.get(pmid);
			
			String docText = article.text_title + "\n" + article.text_abstract;
			
			List<Mention> docGeneMentions = getGeneMentions(docText);
			
			if(docGeneMentions != null){
				mapArtListGenes.put(pmid, docGeneMentions);
			}
		}
		
		System.out.println(String.format("Documents with gene mentions: %d/%d", 
				mapArtListGenes.size(), mapArticles));
		
		return mapArtListGenes;
	}
	
	public static void main(String[] args) throws IOException{
		
       DocGeneFilter doc = new DocGeneFilter();
       
       System.out.println(doc.getGeneMentions("The conserved TRAF3 binding sites in LMP1 and the CD30 Hodgkin's disease marker provides further evidence that a TRAF3-mediated signal transduction pathway may be important in malignant transformation."));
	}
}
