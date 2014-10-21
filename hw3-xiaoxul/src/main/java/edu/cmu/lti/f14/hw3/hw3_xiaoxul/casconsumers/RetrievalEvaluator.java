package edu.cmu.lti.f14.hw3.hw3_xiaoxul.casconsumers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_xiaoxul.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_xiaoxul.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_xiaoxul.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	///** query id number **/
	//public ArrayList<Integer> QIDList;
	///** query and text relevant values **/
	//public ArrayList<Integer> relList;

	
	//qid list
	public LinkedHashSet<Integer> QIDList;
	//qid to token in query
	public HashMap<Integer, Map<String, Integer>> QIDquery;
	//qid to tokens in document with rel=1
	public HashMap<Integer, Map<String, Integer>> QIDdocu1;
	//qid to list of tokens in document with rel=0
	public HashMap<Integer, ArrayList<Map<String, Integer>>>QIDdocu0;
	//qid to score with rel=1
	public HashMap<Integer, Double> QIDscore1;
	//qid to scorewith rel=0
	public HashMap<Integer, ArrayList<Double>> QIDscore0;
	//qid to rank
	public HashMap<Integer, Double> rank;
	//qid to text
	public HashMap<Integer, String> text;

	DecimalFormat ft = new DecimalFormat("#0.0000");

		
	public void initialize() throws ResourceInitializationException {

		QIDList = new LinkedHashSet<Integer>();
		QIDquery = new HashMap<Integer, Map<String, Integer>>();
		QIDdocu1 = new HashMap<Integer, Map<String, Integer>>();
		QIDdocu0 = new HashMap<Integer, ArrayList<Map<String, Integer>>>();
		QIDscore1 = new HashMap<Integer, Double>();
		QIDscore0 = new HashMap<Integer, ArrayList<Double>>();
		rank = new HashMap<Integer, Double>();
		text = new HashMap<Integer, String>();

		//relList = new ArrayList<Integer>();
	}

	/**
	 * TODO :: 1. construct the global word dictionary 
	 * 2. keep the word frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
		
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			ArrayList<Token>tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);

			int qid = doc.getQueryID();
			int rel = doc.getRelevanceValue();
			QIDList.add(qid);
			//relList.add(doc.getRelevanceValue());
			
			//Do something useful here
			
			Map<String, Integer> tokens = new HashMap<String, Integer>();
			Iterator<Token> iter = tokenList.iterator();
			while(iter.hasNext()){
				Token t = (Token)iter.next();
				tokens.put(t.getText(), t.getFrequency());
			}
			if(rel == 99){
				QIDquery.put(qid, tokens);
			}
			else if (rel == 1){
				QIDdocu1.put(qid, tokens);
				text.put(qid, doc.getText());
			}
			else{//rel == 0
				if(!QIDdocu0.containsKey(qid)){
					QIDdocu0.put(qid, new ArrayList<Map<String, Integer>>());		
				}
				QIDdocu0.get(qid).add(tokens);	
			}
		}
	}

	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 
	 * 2. Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);

		// TODO :: compute the cosine similarity measure
		Iterator it = QIDList.iterator();
		while(it.hasNext()){
			int qid = (Integer) it.next(); 
			Map<String, Integer> query = QIDquery.get(qid);
			
			//for rel=1
			Map<String, Integer> doc1 = QIDdocu1.get(qid);
			QIDscore1.put(qid, computeCosineSimilarity(query, doc1)) ;	
			
			
			//for rel=0
			ArrayList<Map<String, Integer>> doc0list = QIDdocu0.get(qid);
			Iterator iter = doc0list.iterator();
			while(iter.hasNext()){
				Map<String, Integer> doc0 = (Map<String, Integer>) iter.next();
				if(!QIDscore0.containsKey(qid)){
					QIDscore0.put(qid, new ArrayList<Double>());
				}
				QIDscore0.get(qid).add(computeCosineSimilarity(query, doc0));
			}	
		}	
		
		// TODO :: compute the rank of retrieved sentences
		for(Integer qid : QIDList){
			Collections.sort(QIDscore0.get(qid), new Comparator<Double>(){
				public int compare(Double d1, Double d2){
					if(d1 < d2)
						return 1;
					else
						return 0;
				}
			});
			
		    Double Score = QIDscore1.get(qid);
		    int i;
		    for(i = 0; i < QIDscore0.get(qid).size(); i++) {
		    	if (Score > QIDscore0.get(qid).get(i)) {
		    		break;	
		        }
		      }
		    rank.put(qid, 1.0 + i);
		}

		for(Integer qid : QIDList){
		    System.out.println("cosine=" + ft.format(QIDscore1.get(qid)) + "\t" + "rank=" + 
		    		rank.get(qid).intValue() + "\t" + "qid=" + qid + "\t" + "rel=1" +"\t" + text.get(qid));
		}

		// TODO :: compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr();
		System.out.println("MRR=" + ft.format(metric_mrr));
		
		write();
	}
	
	public void write() throws IOException{
		File out = new File("report.txt");
		BufferedWriter buf = null;
		try {
			buf = new BufferedWriter(new FileWriter(out));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(Integer qid : QIDList){
			buf.write("cosine=" + ft.format(QIDscore1.get(qid)) + "\t" + "rank=" + 
		    		rank.get(qid).intValue() + "\t" + "qid=" + qid + "\t" + "rel=1" +"\t" + text.get(qid));
			buf.newLine();
		}
		buf.write("MRR=" + ft.format(compute_mrr()));;
		buf.flush();
	}
	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		
		double cosine_similarity = 0.0;
		
		// TODO :: compute cosine similarity between two sentences
		
		double qlen = 0.0;
		
		Set<String> sq  = queryVector.keySet();
		//System.out.println(sq);
		Iterator<String> itq = sq.iterator();
		while(itq.hasNext()){
			String q = itq.next();
			qlen += queryVector.get(q) * queryVector.get(q);
		}
		qlen = Math.sqrt(qlen);
		//System.out.println(qlen);
		double dlen = 0.0;	
		Set<String> sd = docVector.keySet();
		Iterator<String> itd = sd.iterator();
		while (itd.hasNext()){
			String d = itd.next();
			dlen += docVector.get(d) * docVector.get(d);
		}
		dlen = Math.sqrt(dlen);
		//System.out.println(dlen);
		
		double mul = 0.0;
		Set<String> s = queryVector.keySet();
		Iterator it = s.iterator();
		while(it.hasNext()){
			String temp = (String) it.next();
			if(docVector.containsKey(temp)){
				mul += queryVector.get(temp) * docVector.get(temp);
				//System.out.println(mul);
			}
		}
		
		cosine_similarity = mul / qlen / dlen;
		return cosine_similarity;
	}

	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr = 0.0;

		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
		
		Iterator it = QIDList.iterator();
		int count = 0;
		while(it.hasNext()){
			metric_mrr += 1 / rank.get(it.next());
			count++;
		}
		return metric_mrr/count;
	}

}
