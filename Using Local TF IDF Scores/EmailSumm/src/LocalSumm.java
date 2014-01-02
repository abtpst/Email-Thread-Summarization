import java.io.File;
import java.net.URL;
import java.util.*;
import static gate.Utils.inDocumentOrder;
import gate.*;
import static gate.util.persistence.PersistenceManager.loadObjectFromFile;

public class LocalSumm {
	
	 public static String HOME_PATH = "";
	 public LocalSumm() throws Exception{
			Gate.init();
			File gateHome = Gate.getGateHome();
			HOME_PATH = gateHome.getCanonicalPath() + "/";
			
			//System.out.println("Home path: "+ HOME_PATH);
			if (Gate.getGateHome() == null)
				Gate.setGateHome(gateHome);
			
			
			File pluginsHome = new File(gateHome, "plugins");
			//Register all the plugins that your program will need
			Gate.getCreoleRegister().registerDirectories(
					new File(pluginsHome, "ANNIE")
							.toURI().toURL());
	 }
	 
	 @SuppressWarnings("unchecked")
	public static void main(String[] ars)throws Exception
	 {
		 
		new LocalSumm();
		//Point it to where your gapp file resides on your hard drive
		 gate.CorpusController c = ((gate.CorpusController)loadObjectFromFile(new java.io.File("C:/Program Files (x86)/GATE_Developer_7.1/A/1/emailsumm.gapp")));
			Corpus corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
			//Point it to whichever folder contains your documents
			URL dir = new File("C:/Program Files (x86)/GATE_Developer_7.1/A/1/Text").toURI().toURL();
			corpus.populate(dir, null, "UTF-8", false); //set the encoding to whatever is the encoding of your files
			
			c.setCorpus(corpus);
			c.execute();
			
			HashMap<String, Integer> firstnouns = new HashMap<String, Integer>();// HashMap for storing counts of nouns in the first email
			
			Set<String> logins = new HashSet<String>();
			
			ArrayList<String> logs = new ArrayList<String>(); // These two would be used for extracting and storing all the email id's in this thread
			
			HashMap<String, Integer> nouncounter = new HashMap<String, Integer>();// HashMap for storing the counts of all the nouns
			
			HashMap<String, Integer> noundf = new HashMap<String, Integer>();;
			
			List<Annotation> lis = null; // This list would be used to extract all the NVN annotations in the sequence that they appear in the original email
			
			gate.Document tempDoc = null;
			
			int paracount=0;		// We shall keep a count of how many NVN annotations we receive. The we shall use this value to extract the top nouns
			
			//This is how you can access the annotations created by your gate application
			for (int i = 0; i < corpus.size(); i++)
			{
				int b=0;
				
				tempDoc = (gate.Document)corpus.get(i);
				
				for(gate.Annotation ar: tempDoc.getAnnotations().get("EmailID")) // Get all the email ids of the thread, annotated as EmailID
				{
					
					logs =  (ArrayList<String>) ar.getFeatures().get("Login");	// Extract the feature value Login.
					
						for(String l : logs)
							logins.add(l);
			
				}
				
				for(gate.Annotation ar: tempDoc.getAnnotations().get("Email1")) // Get the first email of the thread, annotated as Email1
				{
					firstnouns=(HashMap<String, Integer>) ar.getFeatures().get("NounMap");//Store all the nouns in a HashMap
			
				}
				
				lis = inDocumentOrder(tempDoc.getAnnotations().get("Valid"));		// Extract all the NVN annotations in sequence
				
				for(gate.Annotation a: lis) 
				{
					paracount++;
					HashMap<String, Integer> nounkey = new HashMap<String, Integer>();//This list will store all the nouns associated with a NVN annotation
					
					try
					{
						
					nounkey=(HashMap<String, Integer>) a.getFeatures().get("Nouns");// Get all the nouns associated with this NVN annotation
					
					for (Map.Entry<String, Integer> entry : nounkey.entrySet())// Iterate over all the nouns in this NVN annotation
					{
						String nk = entry.getKey();
						int tf = entry.getValue();
						
						if(!nk.isEmpty() && !logins.contains(nk))// If the noun obtained is not a blank string and it is not an email ID then proceed
						{
							
						if(nouncounter.containsKey(nk))// If this noun already appears in our nouncounter HashMap, then increase its count by 1
							nouncounter.put(nk, nouncounter.get(nk)+tf);
						else// If this noun does not appear in our nouncounter HashMap, meaning this is the first time we see it, set its count to 1
							nouncounter.put(nk, tf);
						
						if(noundf.containsKey(nk))// If this noun already appears in our nouncounter HashMap, then increase its count by 1
							noundf.put(nk, noundf.get(nk)+1);
						else// If this noun does not appear in our nouncounter HashMap, meaning this is the first time we see it, set its count to 1
							noundf.put(nk, 1);
						}
					
					}
					
					
					}
					catch(NullPointerException e)
					{
						
						
					}
				
			}
				//Really important to release these resources or you'll soon run out of working memory
				Factory.deleteResource(tempDoc);
			   
			}
			
			 Factory.deleteResource(corpus);
			
			ArrayList<String> nn = new ArrayList<String>();// This list represents the high scoring nouns that we will select
			
			nn = LocalSumm.Scores(nouncounter, firstnouns, noundf, paracount);//Get the high scoring nouns from the function Scores. Pass the nouncounter, 
		
			ArrayList<String> nvn = new ArrayList<String>();
			
			for(gate.Annotation a: lis) 	//Now iterate over the NVN annotations again, this time collecting the string values of the top nouns that we got above
			{
				HashMap<String, Integer> finalnounkey = (HashMap<String, Integer>) a.getFeatures().get("Nouns");	
				
				for(Map.Entry<String, Integer> entry : finalnounkey.entrySet())
				{
					String n = entry.getKey();
					
					if(nn.contains(n))
						if(!nvn.contains(a.getFeatures().get("Value").toString()))	// Add this string to the final summary only if it has not been seen before
								nvn.add(a.getFeatures().get("Value").toString());
				}
			}
			
			for(String jb : nvn)
			{			
				System.out.println(jb);	// Print out the summary
			}
			
	 }

	public static ArrayList<String> Scores(HashMap<String, Integer> nouncounter, HashMap<String, Integer> firstnouns, HashMap<String, Integer> noundf, double paracount) 
	{
		 Double globalcount =  (paracount/10.0);	// This count represents the number of the nouns which will be our candidates from the sorted HashMap
        	 
         HashMap<String, Double> nounscore = new HashMap<String, Double>();// HashMap for storing the tfidf scores of the nouns in the nouncounter

         for(Map.Entry <String, Integer> entry : nouncounter.entrySet())//Iterate over all the nouns in the nouncounter
         try
         {
        	 String term = entry.getKey();//Get the noun
        	 if(term.matches("[\\p{Alnum}]+"))//Eliminate non alphanumeric nouns
 	 		{ 
        	 int df=1;//df is set to one by default 
        			 
        	 int tf=entry.getValue();// Get the count associated with this noun. This will be the tf
	         
        	 double tfidf;
	         
        	 if(noundf.containsKey(term))
        		 df=noundf.get(term);
        	 
        	 tfidf=tf*Math.log10(paracount/df);//Calculate the tfidf score for this noun
        	
        	 if(firstnouns.containsKey(term))// If this noun also appears in firstnouns then assign it a higher score. 
        		 								//Store the final tfidf score in nounscores
        		 nounscore.put(term, tfidf+1);

        	 else//If not then, store the tfidf score in nounscores
        		 nounscore.put(term, tfidf);
         } 
        	
        }
        
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        ArrayList<String> its = new ArrayList<String>();// This is the list which is returned to the main function. It will hold all the high scoring nouns
      
        // We need to sort the nounscore HashMap by value
        ValueComparator bvc =  new ValueComparator(nounscore);
        
        TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
        
        sorted_map.putAll(nounscore);// sorted_map now has the nounscore HashMap sorted by value in descending order
        System.out.println(globalcount);
        for ( Map.Entry<String, Double> entry : sorted_map.entrySet()) 
		 {System.out.println(entry.getKey()+" "+entry.getValue());
        	if(entry.getValue()>globalcount)//Here we will extract the top values and store them in the ArrayList which would be returned 
			{
        		
				its.add(entry.getKey());// Add the entry to the list its
			}
		 }
		
		return its;
	}
	
	
}

// Sort in descending order
class ValueComparator implements Comparator<String> {

	   Map<String, Double> base;
	   public ValueComparator(Map<String, Double> base) {
	       this.base = base;
	   }

	   // Note: this comparator imposes orderings that are inconsistent with equals.    
	   public int compare(String a, String b) {
	       if (base.get(a) >= base.get(b)) {
	           return -1;
	       } else {
	           return 1;
	       } // returning 0 would merge keys
	   }
	}

//Sort in ascending order
class ValueComparatorAsc implements Comparator<String> {

	   Map<String, Integer> base;
	   public ValueComparatorAsc(Map<String, Integer> base) {
	       this.base = base;
	   }

	   // Note: this comparator imposes orderings that are inconsistent with equals.    
	   public int compare(String a, String b) {
	       if (base.get(a) <= base.get(b)) {
	           return -1;
	       } else {
	           return 1;
	       } // returning 0 would merge keys
	   }
	}