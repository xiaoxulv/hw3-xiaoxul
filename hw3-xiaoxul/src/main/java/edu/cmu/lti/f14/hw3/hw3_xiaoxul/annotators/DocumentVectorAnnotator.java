package edu.cmu.lti.f14.hw3.hw3_xiaoxul.annotators;

import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_xiaoxul.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_xiaoxul.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_xiaoxul.utils.StanfordLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_xiaoxul.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}

	/**
	 * Generate the term frequency for the doc.
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		//String docText = doc.getText();
		//TO DO: construct a vector of tokens and update the tokenList in CAS
		//TO DO: use tokenize0 from above
		
		String text = doc.getText();//.toLowerCase();
		//use the naive tokenize0
		List<String> l = tokenize0(text);
		Iterator<String> it = l.iterator();
		
		//use Stanford Lemmatizer
		//String s = StanfordLemmatizer.stemText(text);
		List<String> L = new LinkedList<String>();
		
		//key = word, value = count 
		HashMap<String, Integer> wordMap = new HashMap<String, Integer>();
		while(it.hasNext()){
			String word = it.next();
			int len = word.length();
			String trueWord = word;
			
			/*not needed not task1
			//get rid of punctuation
			//nonononono number results in bug
			if(word.charAt(len-1) < 'a' || word.charAt(len-1) > 'z'){
				trueWord = word.substring(0, len-1);
				//System.out.println(trueWord);
			}
			*/
			//System.out.println(trueWord);
			//store to map
			if(!wordMap.containsKey(trueWord)){
				wordMap.put(trueWord, 1);
			}
			else{
				wordMap.put(trueWord, 1 + wordMap.get(trueWord));
			}
		}

		Set Tokens = new HashSet();
		Iterator mapit = wordMap.entrySet().iterator();
		//retrieve from hashmap and store in cas (Token:Text,Frequency)
		while(mapit.hasNext()){
			Map.Entry entry = (Map.Entry) mapit.next();
			Token token = new Token(jcas);
			token.setText((String)entry.getKey());
			token.setFrequency((Integer)entry.getValue());
			//System.out.println(token.getText());
			//System.out.println(token.getFrequency());
			Tokens.add(token);
		}
		FSList FSToken = Utils.fromCollectionToFSList(jcas, Tokens);
	    doc.setTokenList(FSToken);
		
	}

}
