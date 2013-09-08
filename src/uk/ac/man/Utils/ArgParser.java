package uk.ac.man.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which is used to parse command-line arguments for a program.
 * Synonyms for a command may be added through the addAlternate(String,String) function.
 * If a --properties &lt;file&gt; command is given, that martin.common.Properties file will be automatically loaded and any key=value pairs in it will be added to this parser. Commands supplied through the command-line will have precedence over any key=value pairs in the properties file.
 * @author Martin
 */
public class ArgParser {
	private static ArgParser parser;

	private String[] nonSwitched;

	private HashSet<String> enabledSwitches = new HashSet<String>();
	private HashMap<String,String[]> followers = new HashMap<String,String[]>();

	public static ArgParser getParser(String[] args){
		if (parser == null)
			parser = new ArgParser(args);

		return parser;
	}

	public static ArgParser getParser(){
		return parser;
	}

	/**
	 * Adds a command synonym, making the two commands equivalent.
	 * @param a command 1 (e.g. "length")
	 * @param b command 2 (e.g. "l")
	 */
	public void addAlternate(String a, String b){
		if (enabledSwitches.contains(a)){
			enabledSwitches.add(b);

			if (followers.containsKey(a))
				followers.put(b,followers.get(a));

		} else if (enabledSwitches.contains(b)){
			enabledSwitches.add(a);

			if (followers.containsKey(b))
				followers.put(a,followers.get(b));
		}
	}

	/**
	 * Will load a properties file from "name" (file or internal), and add it to the argument parser. 
	 * @param p
	 */
	public void addProperties(String name){
		addProperties(Properties.load(name));
	}
	
	/**
	 * Add any key=value pairs in the specified properties object to this parser, where the key is not already specified. 
	 * @param p
	 */
	private void addProperties(Properties p){
		Map<String,String> variables = new HashMap<String,String>();

		for (Object ko : p.keySet()){
			String k = (String) ko;
			String v = (String) p.getProperty(k);
			if (k.startsWith("$"))
				variables.put(k,v);			
		}

		Pattern pattern = Pattern.compile("\\$[a-zA-Z]+");

		for (Object ko : p.keySet()){
			String k = (String) ko;
			if (!enabledSwitches.contains(k)){
				String v = (String) p.getProperty(k);
				enabledSwitches.add(k);

				String[] fs = v.split(";");

				for (int i = 0; i < fs.length; i++){
					Matcher m = pattern.matcher(fs[i]);
					while (m.find()){
						String var = fs[i].substring(m.start(), m.end());
						if (variables.containsKey(var)){
							fs[i] = fs[i].substring(0,m.start()) + variables.get(var) + fs[i].substring(m.end());
							m = pattern.matcher(fs[i]);							
						} else
							throw new IllegalStateException("Unrecognized variable: '" + var + "'");
					}


				}

				followers.put(k, fs);
			}
		}
	}

	/**
	 * Will parse the arguments in args and provide access them through the member functions. 
	 * If --properties <file> is present, the file will be loaded as a Properties file and 
	 * additional arguments will be loaded from it. 
	 * @param args
	 */
	public ArgParser(String[] args){
		ArrayList<String> nonSwitched = new ArrayList<String>();

		for (int i = 0; i < args.length; i++){
			String arg = args[i];

			if (arg.startsWith("--")){
				if (arg.length() > 2){
					String switch_ = arg.substring(2);
					enabledSwitches.add(switch_);

					ArrayList<String> followers = new ArrayList<String>();

					while ((i+1 < args.length) && (!args[i+1].startsWith("-") || Character.isDigit(args[i+1].charAt(1)))){
						i++;
						followers.add(args[i]);
					}

					if (followers.size() > 0){
						this.followers.put(switch_, followers.toArray(new String[0]));
						nonSwitched.addAll(followers);
					}

				} else {
					System.err.println("Invalid argument: " + arg);
					System.exit(-1);
				}
			} else if (arg.startsWith("-") && !Character.isDigit(arg.charAt(1))){
				if (arg.length() > 1){
					if (arg.length() > 2) {

						for (int j = 1; j < arg.length(); j++)
							enabledSwitches.add(""+arg.charAt(j));

					} else {
						String switch_ = arg.substring(1);
						enabledSwitches.add(switch_);

						ArrayList<String> followers = new ArrayList<String>();

						while ((i+1 < args.length) && (!args[i+1].startsWith("-") || Character.isDigit(args[i+1].charAt(1)))){
							i++;
							followers.add(args[i]);
						}

						if (followers.size() > 0){
							this.followers.put(switch_, followers.toArray(new String[0]));
							nonSwitched.addAll(followers);
						}
					}

				} else {
					System.err.println("Invalid argument: " + arg);
					System.exit(-1);
				}
			} else {
				nonSwitched.add(arg);
			}
		}

		this.nonSwitched = nonSwitched.toArray(new String[0]);

		if (this.containsKey("properties"))
			for (String s : this.gets("properties"))
				this.addProperties(Properties.load(s));
	}

	/**
	 * Will print the contents to System.out (mainly for debugging purposes). 
	 */
	public void printContents(){
		System.out.println("\nNonSwitched:");

		for (String s : nonSwitched){
			System.out.println("\t" + s);
		}

		System.out.println("\nEnabled switches:");

		for (String s : enabledSwitches){
			System.out.println("\t" + s);
		}

		System.out.println("\nFollowers:");

		for (String k : followers.keySet()){
			System.out.println("\t" + k);

			for (String s : followers.get(k))
				System.out.println("\t\t" + s);
		}
	}

	/**
	 * @param key
	 * @return whether the specified key has been given.
	 */
	public boolean containsKey(String key){
		return enabledSwitches.contains(key);
	}

	/**
	 * @param key
	 * @return the arguments after key, parsed as doubles
	 */
	public double[] getDoubles(String key){
		if (!followers.containsKey(key))
			return new double[0];

		String[] followers = gets(key);

		double[] retres = new double[followers.length];

		for (int i = 0; i < retres.length; i++)
			retres[i] = Double.parseDouble(followers[i]);

		return retres;
	}

	/**
	 * @param key
	 * @return the arguments after key, parsed as booleans
	 */
	public boolean[] getBooleans(String key){
		if (!followers.containsKey(key))
			return new boolean[0];

		String[] followers = gets(key);

		boolean[] retres = new boolean[followers.length];

		for (int i = 0; i < retres.length; i++)
			retres[i] = Boolean.parseBoolean(followers[i]);

		return retres;
	}

	/**
	 * @param key
	 * @param defaultValue
	 * @return the first boolean value after the key, if none exists return defaultValue
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		Boolean b = getBoolean(key);

		if (b == null)
			return defaultValue;
		else
			return b;
	}

	/**
	 * @param key
	 * @return the first boolean value after the key, if none exists return null
	 */
	public Boolean getBoolean(String key) {
		if (!followers.containsKey(key) || (followers.get(key).length != 1))
			return null;

		return Boolean.parseBoolean(this.get(key));
	}

	/**
	 * @param key
	 * @return the arguments after key, parsed as integers
	 */
	public int[] getInts(String key){
		if (!followers.containsKey(key))
			return new int[0];

		String[] followers = gets(key);

		int[] retres = new int[followers.length];

		for (int i = 0; i < retres.length; i++)
			retres[i] = Integer.parseInt(followers[i]);

		return retres;
	}

	/**
	 *  
	 * @param key
	 * @return the arguments given after key.
	 */
	public String[] gets(String key){
		if (followers.containsKey(key))
			return followers.get(key);
		else
			return new String[0];
	}

	/**
	 * @param key
	 * @return the first String value after the key, if none exists return null
	 */
	public String get(String key){
		if (followers.containsKey(key) && (followers.get(key).length == 1))
			return followers.get(key)[0];
		else
			return null;
	}

	/**
	 * @param key
	 * @param defaultValue
	 * @return the String value after the key, if none exist (or there are more than one argument) return defaultValue
	 */
	public String get(String key, String defaultValue){
		if (followers.containsKey(key) && (followers.get(key).length == 1))
			return followers.get(key)[0];
		else
			return defaultValue;
	}

	/**
	 * @param key
	 * @return the first value after the key interpreted as an integer, if none exists return null
	 */
	public Integer getInt(String key){
		if (!followers.containsKey(key) || (followers.get(key).length != 1))
			return null;

		return Integer.parseInt(get(key));
	}

	/**
	 * @param key
	 * @param defaultValue
	 * @return the first value after the key interpreted as an integer, if none exists return defaultValue
	 */
	public int getInt(String key, int defaultValue){
		Integer x = getInt(key);

		if (x == null)
			return defaultValue;
		else
			return x;
	}

	/**
	 * @param key
	 * @return the first value after the key interpreted as a double, if none exists return null
	 */
	public Double getDouble(String key) {
		if (!followers.containsKey(key) || (followers.get(key).length != 1))
			return null;

		return Double.parseDouble(this.get(key));
	}

	/**
	 * @param key
	 * @param defaultValue
	 * @return the first value after the key interpreted as a double, if none exists return defaultValue
	 */
	public double getDouble(String key, double defaultValue) {
		Double d = getDouble(key);

		if (d == null)
			return defaultValue;
		else 
			return d;
	}

	/**
	 * @param key
	 * @return the arguments after key, interpreted as Files (or an empty array if key does not exist)
	 */
	public File[] getFiles(String key){
		if (!followers.containsKey(key))
			return new File[0];

		String[] followers = gets(key);

		File[] retres = new File[followers.length];

		for (int i = 0; i < retres.length; i++)
			retres[i] = new File(followers[i]);

		return retres;
	}

	/**
	 * @param key
	 * @return the first value after the key interpreted as a File, if none exists return null
	 */
	public File getFile(String key) {
		if (!containsKey(key) || gets(key).length == 0)
			return null;

		return new File(this.get(key));
	}

	/**
	 * @param key
	 * @param defaultValue
	 * @return the first value after the key interpreted as a File, if none exists return defaultValue
	 */
	public File getFile(String key, File defaultValue) {
		File f = getFile(key);

		if (f != null)
			return f;
		else
			return defaultValue;
	}

	public String getRequired(String string) {
		String res = get(string);
		if (res == null)
			throw new IllegalStateException("When running this software, you need to specify exactly one argument for the switch --" + string + ".");
		return res;
	}

	/**
	 * 
	 * @param key
	 * @return an input stream for the given key. If the key starts with "internal:", it will search internally (within the .jar) for the resource. Otherwise it will interpret the key as a file name.
	 */
	public InputStream getInputStream(String key){
		String v = get(key);

		if (v == null)
			return null;

		if (v.startsWith("internal:")){
			InputStream stream = this.getClass().getResourceAsStream(v.substring(9));
			if (stream == null){
				System.out.println("Could not find the internal resource " + v);
				System.exit(0);
			}
			return stream;
		}

		try{
			return new FileInputStream(new File(v));
		} catch (FileNotFoundException e){
			System.out.println("Could not find the file " + v);
			System.exit(0);
		}

		return null;
	}

	/**
	 * 
	 * @param key
	 * @return input streams for the given key. If the key starts with "internal:", it will search internally (within the .jar) for the resource. Otherwise it will interpret the key as a file name.
	 */
	public InputStream[] getInputStreams(String key){
		String[] v = gets(key);

		if (v == null)
			return null;

		InputStream[] res = new InputStream[v.length];
		
		for (int i = 0; i < res.length; i++){
			if (v[i].startsWith("internal:")){
				InputStream stream = this.getClass().getResourceAsStream(v[i].substring(9));
				if (stream == null){
					System.out.println("Could not find the internal resource " + v);
					System.exit(0);
				}
				res[i] = stream;
			} else {
			try{
				res[i] = new FileInputStream(new File(v[i]));
			} catch (FileNotFoundException e){
				System.out.println("Could not find the file " + v[i]);
				System.exit(0);
			}
			}
		}

		return res;
	}

	/**
	 * 
	 * @param key
	 * @param defaultStream
	 * @return an input stream for the given key. If the key starts with "internal:", it will search internally (within the .jar) for the resource. Otherwise it will interpret the key as a file name. 
	 * Will return defaultValue if no value could be found.
	 */
	public InputStream getInputStream(String key, InputStream defaultStream){
		InputStream stream = getInputStream(key);

		if (stream != null)
			return stream;
		else
			return defaultStream;
	}


}
