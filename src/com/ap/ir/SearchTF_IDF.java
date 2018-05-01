package com.ap.ir;

import java.io.File;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

public class SearchTF_IDF {

    public  HashMap<Integer,Float> hm_doc_mod;
    public  HashMap<String,TreeMap<Integer,Integer>> hm_InvertedIndex;
    public  TreeMap<Integer,Float> tm_rank;
    public  IndexReader ir;
    public  SortedMap<Integer,Float> sm_rank;

    SearchTF_IDF() throws Exception
    {
        hm_doc_mod = new HashMap<Integer,Float>();  // HashMap to store document modulus
        hm_InvertedIndex = createFreqencyMap(hm_doc_mod,1); //Get inverted Index & document Modulus
        ir = IndexReader.open(FSDirectory.open(new File("index")));
        if(hm_InvertedIndex==null){System.out.println("Not Enough Memory!!"); System.exit(1);}
    }


    public static void main(String[] args) throws Exception
    {
        System.out.println("Initializing...Task2");
        SearchTF_IDF tfIDF= new SearchTF_IDF();
        tfIDF.task2();
    }


//########################################################################################################################
//	                                              TASK2- Ranking based on TF-IDF
//########################################################################################################################

    public  void task2() throws Exception
    {
        Scanner sc= new Scanner(System.in);
        File f = new File("Task2_result.txt");     //Store Results in to a file.
        PrintWriter pr=new PrintWriter(f);

        while(true)
        {

            System.out.println("Enter the Query (q=Quit):"); //Take Query input from the user.
            String query=sc.nextLine();
            long time = System.currentTimeMillis();	  //Note start time of computation

            if(query.equals("q")||query.equals("Q"))
                break;

            getTopK(ir,query);

            long time1 = System.currentTimeMillis();		//Note End Time.


            long time2 = System.currentTimeMillis();	   //Note start time..

            sortResults();

            long time3 = System.currentTimeMillis();		 // Note End Time of Computation.
            System.out.println("Time to get results:"+(time1-time));
            System.out.println("Time to sort results:"+(time3-time2));

            pr.println("Query:"+query+"\t\tComputation Time:"+(time3-time)+"\t\tRetrieved documents:"+sm_rank.keySet().size());

            printResults(pr,query,10);
        }

        pr.close();
        sc.close();
    }

    public void printResults(PrintWriter pr,String query,int topk) throws Exception
    {
        int top=0;
        for(Integer doc: sm_rank.keySet())
        {
            //System.out.println("\tQuery:"+query+"\tDocument:"+doc+"\tValue:"+tm_rank.get(doc)+"\turl:"+ir.document(doc).getFieldable("path").stringValue().replace("%%", "/"));
            pr.println("["+top+"]"+" Doc:"+doc+"\tVal:"+tm_rank.get(doc)+"\turl:"+ir.document(doc).getFieldable("path").stringValue().replace("%%", "/"));
            top++;
            if(top>=topk) break;
        }
        pr.println("\n\n");
        pr.flush();
    }

    public  void sortResults()
    {
        ValueComparator1 vc = new ValueComparator1(tm_rank);      //Sort Retrieved documents according to similarity.
        sm_rank = new TreeMap<Integer,Float>(vc);
        sm_rank.putAll(tm_rank);
    }

    public  void getTopK(IndexReader ir, String query)
    {
        float query_mod=0;
        HashMap<String,Integer> query_terms= new HashMap<String,Integer>(); //HashMap to store query terms & their frequencies
        tm_rank= new TreeMap<Integer,Float>(); //TreeMap to store all retrieved documents for a given query

        String sterms[]= query.split(" ");

        for(String term : sterms)      //Loop over each term in the query to calculate Numerator Dot Product.
        {
            if(hm_InvertedIndex.get(term)!=null){
                for(Integer doc: hm_InvertedIndex.get(term).keySet())  //Numerator Summation(Wqi*Wdi) computed for all query terms, per document.
                {
                    float value=0;
                    if(tm_rank.get(doc)==null)
                    {value=0;}
                    else
                    {value+=tm_rank.get(doc);}   //As the weight of each term in the query is 1, the dot product is a summation of all TF-IDF values

                    tm_rank.put(doc, value + (float)((hm_InvertedIndex.get(term).get(doc))* Math.log(ir.maxDoc()/hm_InvertedIndex.get(term).keySet().size())));
                }
            }

            Integer termcount=query_terms.get(term);  //Maintain Query vector to calculate Query Modulus |Qi|
            if(termcount==null)
            {query_terms.put(term, 1);}
            else
            {
                termcount=termcount+1;
                query_terms.put(term,termcount);
            }
        }

        for(String str : query_terms.keySet())           //loop to calculate QueryModulus
        {
            query_mod+=query_terms.get(str)*query_terms.get(str);
        }
        query_mod=(float)Math.sqrt(query_mod);


        for(Integer doc:tm_rank.keySet())          //loop to calculate Similarity (Qi.Di)/(|Qi|*|Di|);
        {
            tm_rank.put(doc, tm_rank.get(doc)/(query_mod*hm_doc_mod.get(doc)));
        }

    }



    //########################################################################################################################
//                        Initialize Search.. Pre-compute Document Modulus and Inverted Index
//########################################################################################################################
    public static HashMap<String,TreeMap<Integer,Integer>> createFreqencyMap(HashMap<Integer,Float> hm_doc_mod, int TFflag) throws Exception
    {
        //Get the indexReader
        IndexReader ir= IndexReader.open(FSDirectory.open(new File("index")));
        //HashMap to store inverted index of terms and TreeMap of documents containing the term and their respective frequencies
        HashMap<String,TreeMap<Integer,Integer>> hm_td_freq = new HashMap<String,TreeMap<Integer,Integer>>();
        //TreeMap object reference to store list of document numbers containing the term and corresponding frequency in document
        TreeMap<Integer, Integer> tm_freq;

        int i=0;
        TermEnum sterms = ir.terms(); // Get all terms in the corpus

        long time2 = System.currentTimeMillis();	   //Note start time..
        while(sterms.next())
        {
            int flag=0;float IDF=0;
            i=i+1;
            String word= sterms.term().text();
            Term t = new Term("contents",word);

            TermDocs td = ir.termDocs(t);          //Get all documents in which the term is present.
            tm_freq = new TreeMap<Integer, Integer>();
            if(ir.docFreq(t)>0){IDF=(float)Math.log(ir.maxDoc()/ir.docFreq(t));}
            //if(ir.docFreq(t) >23000)System.out.println("Term:"+word+"\tdocFreq:"+ir.docFreq(t));

            while(td.next())					//Loop & find frequency of the term in each doc
            {
                int doc=td.doc();
                int freq=td.freq();  //get frequency of the term in the doc
                float tfreq=freq;
                if(TFflag==1)        //if TFflag set=1, calculate TF-IDF, per term per doc.
                {
                    tfreq = (float)(freq*IDF);
                }
                flag=1;

                tm_freq.put(doc,freq);

                if(hm_doc_mod.get(doc)==null)  // Store square of frequencies to calculate modulus for each document.
                {
                    hm_doc_mod.put(doc,(float)(Math.pow(tfreq,2)));
                }
                else
                {
                    float value = hm_doc_mod.get(doc);
                    value=value+(tfreq*tfreq);
                    hm_doc_mod.put(doc,value);   //Summation of the square of frequencies.
                }

            }
            if(flag==1)   //few terms not present in any document for them tm_freq=null
            {
                hm_td_freq.put(word, tm_freq);
            }
        }

        for(Integer doc: hm_doc_mod.keySet())   //Calculate modulus of each document
        {
            float value=(float) Math.sqrt(hm_doc_mod.get(doc));
            hm_doc_mod.put(doc, value);
        }
        long time3 = System.currentTimeMillis();		 // Note End Time of Computation.
        System.out.println("Time to compute (Document Norm & Inverted Index):"+(time3-time2)+"ms");

        //System.out.println("Total number Documents:"+ir.maxDoc()+"\tTerms:"+i);
        return hm_td_freq;
    }

}

//########################################################################################################################
//                               Value Comparator Class to sort the results based on similarity.
//########################################################################################################################

class ValueComparator1 implements Comparator<Integer>
{
    SortedMap<Integer, Float> base=new TreeMap<Integer,Float>();
    public ValueComparator1(TreeMap<Integer, Float> b1) {
        this.base.putAll(b1);
    }

    public int compare(Integer d1,Integer d2)                 //To sort the retrieved documents
    {
        if (base.get(d1) <= base.get(d2)) { //Descending sort
            return 1;
        } else {
            return -1;
        } 	//returning 0 would merge keys

    }

}

//########################################################################################################################
//				                              End Of Program.
//########################################################################################################################


