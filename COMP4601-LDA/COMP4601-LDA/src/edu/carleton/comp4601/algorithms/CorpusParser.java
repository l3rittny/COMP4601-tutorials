package edu.carleton.comp4601.algorithms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Logger;

public class CorpusParser {
	// The 5 files in the corpus

	// The file containing the stop words
	public static String STOP_FILE = "stop.txt";
	// The memory-resident array of stop words
	public static ArrayList<String> STOP_WORDS;
	// The memory-resident map of the corpus
	// Each map stores one document in the corpus
	public static ArrayList<HashMap<String, Integer>> MAPS;
	// The master map. It stores the aggregate of reading in
	// all of the documents in the corpus
	public static HashMap<String, Integer> MASTER;
	// The sorted list of words (terms).
	// The ArrayList is sorted by decreasing frequency of word appearance in the corpus
	public static ArrayList<Word> TERMS;
	// Maximum number of terms required in the vector space model
	public static int MAX_TERMS = 5;
	// A friendly logger which is used to output the progress of the input of the corpus
	private final Logger log;
	public static String[] FILE;

	/*
	 * Private class used to facilitate sorting of the master
	 * list of terms. 
	 */
	private class Word implements Comparable<Word> {
		public final String w;		// Word in the corpus
		public final Integer f;		// Word frequency in the corpus
		public final Integer df;		// Word document frequency
		
		public Word(String w, Integer f, Integer df) {
			this.w = w;
			this.f = f;
			this.df = df;
		}

		public int compareTo(Word word) {
			return word.f - f;
		}

		public String toString() {
			return "[" + w + "," + f + "," + df + "]";
		}
	}

	/*
	 * Constructor is used to initialize all of the 
	 * statically allocated maps etc. in the class
	 */
	public CorpusParser() {
		log = Logger.getLogger("CorpusParser");
		MAPS = new ArrayList<HashMap<String, Integer>>();
		MASTER = new HashMap<String, Integer>();
		TERMS = new ArrayList<Word>();
		FILE = new String[5];
		for(int i = 0; i < FILE.length; i++){
			try {
				System.out.println(readFile((i+1) + ".txt"));
				FILE[i] = (i+1) + ".txt";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		input();
	}
	
	private String readFile(String pathname) throws IOException {

	    File file = new File(pathname);
	    StringBuilder fileContents = new StringBuilder((int)file.length());
	    Scanner scanner = new Scanner(file);
	    String lineSeparator = System.getProperty("line.separator");

	    try {
	        while(scanner.hasNextLine()) {
	            fileContents.append(scanner.nextLine() + lineSeparator);
	        }
	        return fileContents.toString();
	    } finally {
	        scanner.close();
	    }
	}

	/*
	 * The main method allows the CorpusParser class to 
	 * be run independently.
	 */
	public static void main(String[] args) {
		CorpusParser parser = new CorpusParser();
		parser.test();
	}

	/*
	 * The test method just tests the sanity of the code
	 */
	public void test() {
		System.out.println("**** TESTING: getDocWordFreq(int,int) ****");
		for (int i = 0; i < FILE.length; i++) {
			System.out.println("Frequency of "+TERMS.get(0)+" in "+FILE[i]+" is "+getDocWordFreq(i, 0));
		}
		
		log.info("**** TESTING: getDocWordFreq(int,String) ****");
		for (int i = 0; i < FILE.length; i++) {
			System.out.println("Frequency of "+TERMS.get(1)+" in "+FILE[i]+" is "+getDocWordFreq(i, TERMS.get(1).w));
		}
		
		log.info("**** TESTING: Compute document vectors) ***");
		for (int i = 0; i < FILE.length; i++) {
			ArrayList<Double> vector = getDocumentVector(i, MAX_TERMS);
			System.out.println(FILE[i]+": "+vector);
		}
		
		log.info("**** TESTING: Compute cosine similarity ***");
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < FILE.length; j++) {
				System.out.println("User "+i+" Document "+j+": "+cosineSimilarity(i, j));
			}
		}
	}
	
	/* 
	 * The readStopWords method reads in all of the words which
	 * are considered stop words (e.g., "a", "is"). There are 119
	 * words in the stop.txt file.
	 */
	private void readStopWords() {
		try {
			Scanner s = new Scanner(new File(STOP_FILE));
			STOP_WORDS = new ArrayList<String>();
			while (s.hasNext()) {
				STOP_WORDS.add(s.next());
			}
			s.close();
			log.info("Read in " + STOP_WORDS.size() + " stop words.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* 
	 * The readWords method computes all of the word maps across 
	 * all of the 5 documents in the corpus. HashMaps are used 
	 * for each document in the corpus in order to facilitate retrieval.
	 */
	private void readWords() {
		try {
			// Now read in all of the words in the 5 files
			for (String file : FILE) {
				int noWords = 0;
				int distinctWords = 0;
				HashMap<String, Integer> map = new HashMap<String, Integer>();
				MAPS.add(map);
				// Read in all of the words in the file
				Scanner s = new Scanner(new File(file));
				while (s.hasNext()) {
					String word = s.next().toLowerCase();
					noWords++;
					if (map.containsKey(word)) {
						map.put(word, map.get(word) + 1);
					} else {
						distinctWords++;
						map.put(word, 1);
					}
					if (MASTER.containsKey(word)) {
						MASTER.put(word, MASTER.get(word) + 1);
					} else {
						MASTER.put(word, 1);
					}
				}
				// Remove all of the words that we don't care about
				for (String stopWord : STOP_WORDS) {
					map.remove(stopWord);
					MASTER.remove(stopWord);
				}
				s.close();
				log.info(file + " processed. It contained " + noWords
						+ " words (" + distinctWords + "). There are "
						+ map.keySet().size() + " non-stop words.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * This computes the terms of use in the 5 documents
	 * The TERMS ArrayList is sorted in descending frequency 
	 * So, TERMS.get(0) returns the Word with the highest frequency
	 * across all of the 5 documents. The Word object also stores
	 * the document frequency for the term.
	 */
	private void sortTerms() {
		for (Entry<String, Integer> e : MASTER.entrySet()) {
			int df = 0;
			for (int i = 0; i < FILE.length; i++)
				if (MAPS.get(i).containsKey(e.getKey()))
					df++;
			TERMS.add(new Word(e.getKey(), e.getValue(), df));
		}
		Collections.sort(TERMS);
		log.info(TERMS.toString());
	}

	/* 
	 * The input method simply reads in the stop words, the documents
	 * in the corpus (5 txt files) and then computes a sorted list of 
	 * terms which can be used to construct a vector space model.
	 * Simply accessing the first MAX_TERMS entries of the TERMS ArrayList
	 * (potentially) allows a vector space model to be constructed.
	 */
	private void input() {
		readStopWords();
		readWords();
		sortTerms();
	}
	
	/*
	 * The getDocument method returns a HashMap of the requested document.
	 * Together with TERMS it allows particular document word frequencies to
	 * be retrieved. 
	 */
	public static HashMap<String, Integer> getDocument(int index) {
		if (index < 0 || index > MAPS.size())
			return null;
		return MAPS.get(index);
	}
	
	/*
	 * The getDocWordFreq method allows a document to be queried for a
	 * particular frequency of a term (which would have been obtained from
	 * the TERMS ArrayList.
	 * 
	 * Given a wordID (typically 0,1,2,3,4 for the vector space model) and a
	 * docID (here 0,1,2,3,4 for 1.txt, 2.txt etc.) we can get the frequency of the 
	 * word in the document.
	 */
	public static int getDocWordFreq(int docID, int wordID) {
		if (wordID < 0 || wordID > TERMS.size())
			return 0;
		if (docID < 0 || docID > FILE.length)
			return 0;
		Word word = TERMS.get(wordID);
		HashMap<String,Integer> doc = getDocument(docID);
		Integer freq = doc.get(word.w);
		if (freq == null)
			return 0;
		else
			return freq;
	}
	
	/*
	 * The getDocWordFreq allows a document to be queried for 
	 * the frequency of a specific word.
	 */
	public static int getDocWordFreq(int docID, String word) {
		if (word == null)
			return 0;
		if (docID < 0 || docID > FILE.length)
			return 0;
		HashMap<String,Integer> doc = getDocument(docID);
		Integer freq = doc.get(word);
		if (freq == null)
			return 0;
		else
			return freq;		
	}
	
	/*
	 * Compute the idf value for a given term
	 */
	public static double idf(int wordID) {
		return Math.log10(FILE.length * 1.0 / TERMS.get(wordID).df);
	}
	
	/* 
	 * Compute idf weight for a given term in a specific document
	 */
	public static double tf(int docID, int wordID) {
		return Math.log10(1.0+getDocWordFreq(docID, wordID));
	}
	
	/*
	 * Compute the tf-idf weight of a term in a document
	 */
	public static double tf_idf(int docID, int wordID) {
		return tf(docID, wordID) * idf(wordID);
	}
	
	/* 
	 * Compute the document vector. The size allows us to take as 
	 * many terms as we like (up to the maximum number available in
	 * the corpus).
	 */
	public static ArrayList<Double> getDocumentVector(int docID, int size) {
		ArrayList<Double> vector = new ArrayList<Double>();
		for (int i = 0; i < size; i++) {
			vector.add(tf_idf(docID, i));
		}
		return vector;
	}
	
	/* 
	 * Compute cosine similarity
	 * 
	 */
	public static double cosineSimilarity(int userID, int docID) {
		double bQ = 0;
		double bD = 0;
		double top = 0;
		ArrayList<Double> vector = getDocumentVector(docID, MAX_TERMS);
		for (int i = 0; i < MAX_TERMS; i++) {
			top += USERS[userID][i] * vector.get(i);
			bQ += USERS[userID][i] * USERS[userID][i];
			bD += vector.get(i) * vector.get(i);
		}
		
		return top / (Math.sqrt(bQ) * Math.sqrt(bD));
	}
	
	/*
	 * Provided users
	 */
	public static double[][] USERS = {
		{ 1.0, 0.0, 0.0, 0.0, 0.0 },
		{ 0.5, 0.0, 0.5, 0.0, 0.5 },
		{ 1.0, 0.8, 0.6, 0.4, 0.2 },
		{ 0.5, 0.0, 0.25, 0.0, 0.5 },
		{ 1.0, 0.6, 0.8, 0.4, 0.2 }
	};
}
