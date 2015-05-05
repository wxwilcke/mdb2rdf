import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Config {
	private JSONObject CONFIGURATION;

	public Config () {
		CONFIGURATION	= new JSONObject();
	}

	public Config (String config) {
		this();
		load(config);
	}

	private void load (String config) {
		JSONParser parser = new JSONParser();

		try {
			CONFIGURATION = (JSONObject) parser.parse(new FileReader(config));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public Object get(String key) {
		if (CONFIGURATION.containsKey(key)) {
			return CONFIGURATION.get(key);
		}

		return null;
	}
	
	public String toString() {
		String result	= new String();

		for(Iterator<?> iterator = CONFIGURATION.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				result += new String(key + ":" + CONFIGURATION.get(key));

				if (iterator.hasNext()) result += ", ";
			}

		return result;
	}
}
