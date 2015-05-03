import com.healthmarketscience.jackcess.*;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

// assumption made is that the primary key is an integer (LONG)
public class RdbToRdf {
	private String DB_PATH; // path pointing to the database
	private Config CONFIG; // configuration
	private Model model;

	/*
	 * Constructors
	 */
	public RdbToRdf() {
		DB_PATH		= null;
		CONFIG		= null;
		model 		= ModelFactory.createDefaultModel();
	}

	public RdbToRdf(String db, Config config) {
		this();
		setDatabase(db);
		setConfig(config);
	}
	

	/*
	 * Setters and Getters
	 */

	public void setDatabase(String db) {
		this.DB_PATH = db;
	}
	public String getDatabase() {
		return DB_PATH;
	}
	
	public void setConfig(Config config) {
		this.CONFIG = config;
	}
	public Config getConfig() {
		return CONFIG;
	}

	/* 
	 * Core functions
	 */

	public void convert() {
		try {
			Database db = DatabaseBuilder.open(new File(getDatabase()));
			List<String> tables	= getTables(db); 

			for (String tableName : tables) {
				Table table = db.getTable(tableName);
			
				for(Row row : table) {
					int id			= row.getInt(getPrimaryKeyIndex(table)); 
					Resource rcs	= ResourceFactory.createResource(genMRURI(tableName, String.valueOf(id)));

					// link resource to existing resource if already present
					// in model
/*					if (model.containsResource(rcs)) {
						Resource existingRcs	= model.getResource(rcs.getURI());
						Property p				= ResourceFactory.createProperty(new String(NAMESPACE + "#" + tableName));
						Statement s				= ResourceFactory.createStatement(existingRcs, p, rcs);

						// add statement if not already present
						if (!model.contains(s)) {
							model.add(s);
						}
					} */

					// create and add all attributes of resource
					Statement[] sa	= convertRowToStatement(row, rcs, id);
					addRowToModel(sa);
				}
			}

			db.close();

		} catch (IOException e) {
			// nothing
		}
	
		model.write(System.out, (String) getConfig().get("outputFormat"));
	}

	// add every statement to the model iff it is not already present
	private void addRowToModel(Statement[] sa) {
		for (int i = 0; i < sa.length; i++) {
			if (model.contains(sa[i])) {
				continue;
			}
			model.add(sa[i]);
		}
	}


	// return the tables within the database, minus those explicitly excluded
	private List<String> getTables(Database db) {
		List<String> tables	= new ArrayList<String>();
		List<String> excluded	= (List<String>) getConfig().get("excludedTables");

		try {
			tables = new ArrayList<String>(db.getTableNames());
			if (!excluded.isEmpty()) {
				for (String excl : excluded) {
					tables.remove(excl);
				}
			}
		} catch (IOException e) {
			// nothing
		}

		// move main table to front of list
		tables.remove((String) getConfig().get("masterTable"));
		tables.add(0, (String) getConfig().get("masterTable"));

		return tables;
	}


	// create an array of statements from the attribute-value pairs within the
	// row
	private Statement[] convertRowToStatement(Row row, Resource rcs, int id) {
		Statement[] sa	= new Statement[row.size()];
		int i = 0;
		
		Set<String> attrs = row.keySet();
		for (String attr : attrs) {
			if (attr.equals(id)) { // omit primary key from becoming a node
				continue;
			}
			
			Resource attrRcs;
			Object value	= row.get(attr);
			if (value == null) { // dealing with empty values
				attrRcs	= ResourceFactory.createResource(); //bnode
			} else {
				attrRcs	= ResourceFactory.createResource(genRURI(value.toString()));
			}

			Property p	= ResourceFactory.createProperty(genOURI(attr.toString()));
			Statement s	= ResourceFactory.createStatement(rcs, p, attrRcs);
			
			sa[i++]	= s;
		}

		return sa;
	}

	//generate master resource uri
	private String genMRURI(String name, String id) {
		return (new String((String) getConfig().get("namespaceResource") + (String) getConfig().get("delimeterResource") + name + "_" + id));
	}
	
	//generate resource uri
	private String genRURI(String value) {
		return (new String((String) getConfig().get("namespaceResource") + (String) getConfig().get("delimeterResource") + value));
	}
	
	//generate ontology uri
	private String genOURI(String property) {
		return (new String((String) getConfig().get("namespaceOntology") + (String) getConfig().get("delimeterOntology") + property));
	}

	// determine and return the primary key of a table
	private String getPrimaryKeyIndex(Table table) {
		// return table.getPrimaryKeyIndex().getName();
		return "ID"; //ugly fix to circumvent jackcess bug
	}
}
