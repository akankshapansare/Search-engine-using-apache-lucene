package com.ap.ir;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * Created by akanksha on 1/1/2018.
 */
public class TF_IDF {

    HashMap<String, HashMap<Integer, Integer>> tfFrequencyMap = new HashMap<>();
    HashMap<String, Integer> queryMap = new HashMap<>();
    HashMap<Integer, Float> docModMap = new HashMap<Integer, Float>();  // HashMap to store document modulus
    HashMap<Integer, Float> resultMap = new HashMap<>();
    IndexReader lucene;

    public static void main(String[] args) throws Exception {
        System.out.print("Enter the query");
        Scanner scanner = new Scanner(System.in);
        String query = scanner.nextLine();

        TF_IDF tf = new TF_IDF();
        tf.getQueryHashMap(query);
        tf.createTermFrequencyMap();
        tf.search();
        tf.printResult();

    }

    private void getQueryHashMap(String query) {

        String words[] = query.split(" ");

        for (String term : words) {
            if (!queryMap.containsKey(term)) {
                queryMap.put(term, 1);
            } else {
                int value = queryMap.get(term);
                queryMap.put(term, value + 1);
            }
        }
    }

    private void search() {

        //For query Modulus
        float queryMod = 0;
        for (String str : queryMap.keySet()) {
            queryMod += queryMap.get(str) * queryMap.get(str);
        }
        queryMod = (float) Math.sqrt(queryMod);

        //For document modulus
        //Calculated in creatTermFrequencyMap method


        //For query and document dot product
        HashSet<Integer> queryDocuments = new HashSet<>();
        for (String word : queryMap.keySet()) {
            queryDocuments.addAll(tfFrequencyMap.get(word).keySet());
        }

        for (Integer docId : queryDocuments) {
            float neumerator = 0;
            for (String word : queryMap.keySet()) {
                neumerator = neumerator + tfFrequencyMap.get(word).get(docId) * queryMap.get(word);
            }

            resultMap.put(docId, neumerator / (queryMod * docModMap.get(docId)));
        }
    }

    private void printResult() {
        for (Integer doc : resultMap.keySet()) {
            System.out.println(doc + " " + resultMap.get(doc));
        }
    }

    private void createTermFrequencyMap() throws Exception {

        //Get the indexReader
        lucene = IndexReader.open(FSDirectory.open(new File("index")));


        //HashMap object reference to store list of document numbers containing the term and corresponding frequency in document
        HashMap<Integer, Integer> tm_freq;

        TermEnum sterms = lucene.terms(); // Get all terms in the corpus

        while (sterms.next()) {
            String word = sterms.term().text();

            Term t = new Term("contents", word);

            TermDocs td = lucene.termDocs(t);          //Get all documents in which the term is present.
            tm_freq = new HashMap<>();

            while (td.next()) {
                int doc = td.doc();
                int freq = td.freq();  //get frequency of the term in the doc
                float tfreq = freq;
                tm_freq.put(doc, freq);

                // Store square of frequencies to calculate modulus for each document.
                if (docModMap.get(doc) == null) {
                    docModMap.put(doc, (float) (Math.pow(tfreq, 2)));
                } else {
                    float value = docModMap.get(doc);
                    value = value + (tfreq * tfreq);
                    docModMap.put(doc, value);   //Summation of the square of frequencies.
                }
            }
            tfFrequencyMap.put(word, tm_freq);
        }

        //Calculate modulus of each document
        for (Integer doc : docModMap.keySet()) {
            float value = (float) Math.sqrt(docModMap.get(doc));
            docModMap.put(doc, value);
        }

    }

}
