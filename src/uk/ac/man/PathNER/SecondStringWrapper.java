package uk.ac.man.PathNER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.wcohen.ss.lookup.SoftTFIDFDictionary;

public class SecondStringWrapper {
	public static SoftTFIDFDictionary dict = null;
	
	static public void init(String fileName) throws IOException,ClassNotFoundException
    {
        long start0 = System.currentTimeMillis();
        if (fileName.endsWith(".list")) {
            System.out.println("loading aliases...");
            dict = new SoftTFIDFDictionary();
            dict.loadAliases(new File(fileName));
        } else {
            System.out.println("restoring...");
            dict = SoftTFIDFDictionary.restore(new File(fileName));
        }
        double elapsedSec0 = (System.currentTimeMillis()-start0) / 1000.0;
        System.out.println("loaded in "+elapsedSec0+" sec");
        long start1 = System.currentTimeMillis();
        System.out.println("freezing...");
        dict.freeze();
        double elapsedSec1 = (System.currentTimeMillis()-start1) / 1000.0;
        System.out.println("SecondString nitialized");
        //System.out.println("frozen in "+elapsedSec1+" sec");
        //System.out.println("total i/o "+(elapsedSec1+elapsedSec0)+" sec");
    }
	
	public static void main(String[] args) throws Exception{
		if (args.length == 0) {
			System.out
					.println("usage 1: aliasfile threshold query1 query2 ... - run queries");
			System.out
					.println("usage 2: aliasfile threshold queryfile - run queries from a file");
			System.out
					.println("usage 3: aliasfile window1 window2 .... - explore different window sizes");
			System.out
					.println("usage 4: aliasfile savefile - convert to fast-loading savefile");
			System.out.println("usage 4: aliasfile - print some stats");
			System.exit(0);
		}

		String query = "endocytosis";
		
		init(args[0]);
		double d = Double.parseDouble(args[1]);
		int result = dict.lookup(d, query);

		for (int i = 0; i < result; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append(dict.getResult(i)).append("\t").append(dict.getScore(i)).append("\t").append(dict.getValue(i));
			System.out.println(sb.toString());
		}
	}
}
