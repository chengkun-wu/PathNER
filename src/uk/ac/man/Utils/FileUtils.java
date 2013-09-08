package uk.ac.man.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.io.input.BOMInputStream;

/*
 * Author: Chengkun Wu
 */

public class FileUtils {
	public static BufferedReader br = null;
	public static BufferedWriter bw = null;
	public static File file = null;
	public static FileWriter fw = null;
	
	public static void str2File(String str, String filename){
		try {
			file = new File(filename);
			bw = new BufferedWriter(new OutputStreamWriter
  	 			  (new FileOutputStream(file),"UTF-8"));

			// Start writing to the output stream

			bw.write(str);
			bw.newLine();
			bw.flush();

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			// Close the BufferedWriter
			try {
				if (bw != null) {
					bw.flush();
					bw.close();

					System.out.println("File successfully written to "+ file.getAbsolutePath());
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void strVector2File(Vector<String> strVector, String filename){
	     try {
	    	 	file = new File(filename);
	    	 	//fw = new FileWriter(file);
	    	 	bw = new BufferedWriter(new OutputStreamWriter
	    	 			  (new FileOutputStream(file),"UTF-8"));
	            
	            //Start writing to the output stream
	           
	    	 	for (int i = 0; i < strVector.size(); i++){
	    	 		bw.write(strVector.get(i));
	    	 		bw.newLine();
	    	 		bw.flush();
	    	 	}
	            
	        } catch (FileNotFoundException ex) {
	            ex.printStackTrace();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        } finally {
	            //Close the BufferedWriter
	            try {
	                if (bw != null) {
	                    bw.flush();
	                    bw.close();
	                    
	                    System.out.println("File successfully written to " + file.getAbsolutePath());
	                }
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
	}
	
	public static void strList2File(List<String> strList, String filename){
	     try {
	    	 	file = new File(filename);
	    	 	//fw = new FileWriter(file);
	    	 	bw = new BufferedWriter(new OutputStreamWriter
	    	 			  (new FileOutputStream(file),"UTF-8"));
	            
	            //Start writing to the output stream
	           
	    	 	for (int i = 0; i < strList.size(); i++){
	    	 		bw.write(strList.get(i));
	    	 		bw.newLine();
	    	 		bw.flush();
	    	 	}
	            
	        } catch (FileNotFoundException ex) {
	            ex.printStackTrace();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        } finally {
	            //Close the BufferedWriter
	            try {
	                if (bw != null) {
	                    bw.flush();
	                    bw.close();
	                    
	                    System.out.println("File successfully written to " + file.getAbsolutePath());
	                }
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
	}
	
	public static void strCollection2File(Collection<String> strCollection, String filename){
	     try {
	    	 	file = new File(filename);
	    	 	//fw = new FileWriter(file);
	    	 	bw = new BufferedWriter(new OutputStreamWriter
	    	 			  (new FileOutputStream(file),"UTF-8"));
	            
	            //Start writing to the output stream
	           
	    	 	for (String str: strCollection){
	    	 		bw.write(str);
	    	 		bw.newLine();
	    	 		bw.flush();
	    	 	}
	            
	        } catch (FileNotFoundException ex) {
	            ex.printStackTrace();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        } finally {
	            //Close the BufferedWriter
	            try {
	                if (bw != null) {
	                    bw.flush();
	                    bw.close();
	                    
	                    System.out.println("File successfully written to " + file.getAbsolutePath());
	                }
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
	}
	
	public static void intVector2File(Vector<Integer> intVector, String filename){
	     try {
	    	 	file = new File(filename);
	    	 	fw = new FileWriter(file);
	            Writer writer = new OutputStreamWriter(
	                       new FileOutputStream(filename), "UTF-8");
	            BufferedWriter bw = new BufferedWriter(writer);
	            
	            //Start writing to the output stream
	           
	    	 	for (int i = 0; i < intVector.size(); i++){
	    	 		bw.write(""+intVector.get(i));
	    	 		bw.newLine();
	    	 		bw.flush();
	    	 	}
	            
	        } catch (FileNotFoundException ex) {
	            ex.printStackTrace();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        } finally {
	            //Close the BufferedWriter
	            try {
	                if (bw != null) {
	                    bw.flush();
	                    bw.close();
	                    
	                    System.out.println("File successfully written to " + file.getAbsolutePath());
	                }
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
	}
	
	public static Vector<String> file2StrVector(String filename){
		Vector<String> strlines = new Vector<String>();
		
		strlines.addAll(file2StrList(filename));
		
		return strlines;
	}
	
	public static List<String> file2StrList(String filename){
		List<String> strlines = new ArrayList<String>();
		
		file = new File(filename); 
		
		if (file == null || !file.exists() != false) {
			System.out.println(filename + " not exists");
			return null;
		}
		
		 try {  
	            FileReader fr = new FileReader(file);  
	            BOMInputStream bomIn = new BOMInputStream(new FileInputStream(file));

	    		int firstNonBOMByte = bomIn.read(); // Skips BOM
	    		if (bomIn.hasBOM()) {
	    		    System.out.println("BOM detected and removed!");
	    		}
	    		bomIn.close();
	    		
	            br = new BufferedReader(new InputStreamReader
	    	 			  (new FileInputStream(file),"UTF-8"));
	    		//br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
	            
	         //   br = new BufferedReader(fr);
	            String line = null;
	            while (( line = br.readLine()) != null) {  
	               // System.out.println(line);  
	                strlines.add(line);
	            }  
	        } catch (Exception e) {  
	            e.printStackTrace();  
	        }finally {  
	            try {  
	                br.close();  
	            } catch (IOException e) {  
	                e.printStackTrace();  
	            }  
	        }  
		 	
		return strlines;
	}
	
	public static Collection<String> file2StrCollection(String filename){
		Collection<String> strlines = new TreeSet<String>();
		
		file = new File(filename); 
		
		if (file == null || !file.exists() != false) {
			System.out.println(filename + " not exists");
			return null;
		}
		
		 try {  
	            FileReader fr = new FileReader(file);  
	            BOMInputStream bomIn = new BOMInputStream(new FileInputStream(file));

	    		int firstNonBOMByte = bomIn.read(); // Skips BOM
	    		if (bomIn.hasBOM()) {
	    		    System.out.println("BOM detected and removed!");
	    		}
	    		bomIn.close();
	    		
	            br = new BufferedReader(new InputStreamReader
	    	 			  (new FileInputStream(file),"UTF-8"));
	            
	         //   br = new BufferedReader(fr);
	            String line = null;
	            while (( line = br.readLine()) != null) {  
	               // System.out.println(line);  
	                strlines.add(line);
	            }  
	        } catch (Exception e) {  
	            e.printStackTrace();  
	        }finally {  
	            try {  
	                br.close();  
	            } catch (IOException e) {  
	                e.printStackTrace();  
	            }  
	        }  
		
		return strlines;
	}
	
	
}
