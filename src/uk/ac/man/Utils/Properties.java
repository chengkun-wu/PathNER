package uk.ac.man.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extended version of they java.util.Properties class. 
 * @author Martin
 */
public class Properties extends java.util.Properties {

	public static final long serialVersionUID = 0; 

	public Properties(){
		super();
	}

	public Properties(Properties defaults){
		super(defaults);
	}

	public boolean getBooleanProperty(String key){
		String v = this.getProperty(key);

		if (v == null)
			throw new IllegalStateException("No value could be found for the key " + key);

		v = v.toLowerCase();

		if (v.equals("yes") || v.equals("true") || v.equals("1"))
			return true;
		if (v.equals("no") || v.equals("false") || v.equals("0"))
			return false;

		throw new IllegalStateException("Value '" + v + "' for key '" + key + "' was not recognized as a boolean.");
	}

	public boolean getBooleanProperty(String key, boolean defaultvalue){
		String v = this.getProperty(key);

		if (v == null)
			return defaultvalue;

		v = v.toLowerCase();

		if (v.equals("yes") || v.equals("true") || v.equals("1"))
			return true;
		if (v.equals("no") || v.equals("false") || v.equals("0"))
			return false;

		throw new IllegalStateException("Value '" + v + "' for key '" + key + "' was not recognized as a boolean.");
	}
	public Object setBooleanProperty(String key, boolean value){
		if (value == true)
			return this.setProperty(key, "yes");
		else
			return this.setProperty(key, "no");
	}

	public int getIntegerProperty(String key){
		String v= this.getProperty(key);

		if (v == null)
			throw new IllegalStateException("No value could be found for the key " + key);

		return Integer.parseInt(this.getProperty(key));
	}

	public int getIntegerProperty(String key, int defaultvalue){
		String v= this.getProperty(key);

		if (v == null)
			return defaultvalue;

		return Integer.parseInt(this.getProperty(key));
	}

	public Object setIntegerProperty(String key, int value){
		return this.setProperty(key, ""+value);
	}


	public double getDoubleProperty(String key){
		String v= this.getProperty(key);

		if (v == null)
			throw new IllegalStateException("No value could be found for the key " + key);

		return Double.parseDouble(this.getProperty(key));
	}

	public double getIntegerProperty(String key, double defaultvalue){
		String v= this.getProperty(key);

		if (v == null)
			return defaultvalue;

		return Double.parseDouble(this.getProperty(key));
	}

	public Object setDoubleProperty(String key, double value){
		return this.setProperty(key, ""+value);
	}

	public static Properties load(String filename){
		return Properties.load(filename, null);
	}

	public static Properties load(String filename, Properties defaultProperties){
		try{
			if (filename.startsWith("internal:")){
				InputStream inStream = new Properties().getClass().getResourceAsStream(filename.substring(9));

				if (inStream == null){
					System.err.println("Could not find internal resource " + filename);
					System.exit(-1);
				}
				
				Properties p = load(inStream,defaultProperties);
				inStream.close();
				return p;
			} else {
				InputStream inStream = new FileInputStream(new File(filename));
				Properties p = load(inStream,defaultProperties);
				inStream.close();
				return p;
			}
		} catch (FileNotFoundException e){
			System.err.println("Could not find properties file: " + filename);
			System.exit(-1);
		} catch (IOException e){
			System.err.println("IO Exception: " + e);
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	public static Properties load(InputStream stream, Properties defaultProperties){
		Properties p;

		if (defaultProperties == null)
			p = new Properties();
		else
			p = new Properties(defaultProperties);

		try{
			p.load(stream);
		} catch (IOException e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}

		return p;
	}
}
