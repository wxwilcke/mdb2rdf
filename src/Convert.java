/*
 * Generic RDB to RDF Conversion Tool
 *
 * Xander Wilcke
 * VU University Amsterdam
 * 
 */

import java.util.List;
import java.util.ArrayList;

public class Convert {
	private static Double VERSION	= 0.1;

	public static void main(String[] args) {
		List<String> d	= new ArrayList<String>();
		List<String> c	= new ArrayList<String>();

		String flag = new String();
		for(int count = 0; count < args.length; count++) {
			// are we in a flag block?
			if (args[count].startsWith("-")) {
				flag = args[count];
				continue;
			}
			
			if (flag.equals("-v")) printVersion();

			if (flag.equals("-d")) d.add(args[count]);
			if (flag.equals("-c")) c.add(args[count]);
		}

		//check for compulsory flags
		if (d.isEmpty() || c.isEmpty()) {
			System.out.println("Compulsory flags must be given");
			return;
		}

		Config config		= new Config(c.get(0));
		RdbToRdf converter	= new RdbToRdf(d.get(0), config);

		converter.convert();

	}

	private static void printVersion() {
		System.out.println(VERSION);

		System.exit(0);
	}
}
