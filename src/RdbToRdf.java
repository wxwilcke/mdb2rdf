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
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.ResIterator;

// assumption made is that the primary key is an integer (LONG)
public class RdbToRdf {
	private String DB_PATH; // path pointing to the database
	private Config CONFIG; // configuration
	private Model MODEL;

	/*
	 * Constructors
	 */
	public RdbToRdf() {
		DB_PATH		= null;
		CONFIG		= null;
		MODEL 		= ModelFactory.createDefaultModel();
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
					String key		= getPrimaryKeyIndex(table);
					Resource rcs	= ResourceFactory.createResource(genMRURI(tableName, String.valueOf(row.getInt(key))));


					// create and add all attributes of resource
					List<Statement> sa	= convertRowToStatement(table, row, rcs);
					addRowToMODEL(sa, key, tableName);
				}
			}

			db.close();

		} catch (IOException e) {
			// nothing
		}
	
		MODEL.write(System.out, (String) getConfig().get("outputFormat"));
	}

	// add every statement to the MODEL iff it is not already present
	private void addRowToMODEL(List<Statement> sa, String key, String puri) {
		for (Statement s : sa) {
			if (MODEL.contains(s)) {
				continue;
			}
			// add to existing resource with same key if exists
			if (s.getPredicate().getLocalName().equals(key)) {
				ResIterator it	=	MODEL.listResourcesWithProperty(s.getPredicate(), s.getObject());
				if (it.hasNext()) { // assume all members are equal
					Resource rsc	= it.nextResource(); // get parent
					Property p	= ResourceFactory.createProperty(genOURI(), puri);
					Statement st	= ResourceFactory.createStatement(rsc, p, s.getSubject());

					MODEL.add(st);

					continue;
				}
			}

			MODEL.add(s);
		}
	}


	// return the tables within the database, minus those explicitly excluded
	private List<String> getTables(Database db) {
		List<String> tables	= new ArrayList<String>();
		@SuppressWarnings("unchecked") List<String> excluded	= (List<String>) getConfig().get("excludedTables");

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
	private List<Statement> convertRowToStatement(Table table, Row row, Resource rcs) {
		List<Statement> sa	= new ArrayList<Statement>(row.size()); 
		int i = 0;
		
		Set<String> attrs = row.keySet();
		for (String attr : attrs) {
			RDFNode attrRcs;
			Object value	= row.get(attr);
			if (value == null) { // dealing with empty values
				attrRcs	= ResourceFactory.createResource(); //bnode
			} else {
				attrRcs	= createRDFNode(table.getColumn(attr), value);
			//	attrRcs	= ResourceFactory.createResource(genRURI(value.toString()));
			}

			Property p	= ResourceFactory.createProperty(genOURI(), attr.toString());
			Statement s	= ResourceFactory.createStatement(rcs, p, attrRcs);
			
			sa.add(s);
		}

		return sa;
	}

	private RDFNode createRDFNode(Column column, Object value) {
		RDFNode node	= ResourceFactory.createResource(); //bnode
		
		DataType t	= column.getType();
		switch(t) {
			case TEXT:	
			case MEMO:
				if (isLiteralValue(value.toString())) {
					// TODO: add language option
					node	= ResourceFactory.createPlainLiteral(value.toString());
					break;
				}

				node	= ResourceFactory.createResource(resolveURI(value.toString()));
				break;

			default:
				// TODO: add datatype
				node	= ResourceFactory.createPlainLiteral(value.toString());
				break;
		}

		return node;
	}

	// heuristical method to determine whether it is a literal or not
	private Boolean isLiteralValue(String s) {
		return (isSentence(s) || isURL(s));
	}

	private Boolean isSentence(String s) {
		//return s.matches("[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?\\s|$)[^.!?]*)*[.!?]?['\"]?(?=\\s|$)");
		return s.matches("[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?$)[^.!?]*)*[.!?]?['\"]?(?=\\s|$)");
	}

	private Boolean isURL(String s) {
		// source: mathiasbynens.be/demo/url-regex
		return s.matches("_^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$_iuS");
	}

	private String resolveURI(String value) {
		String uri	= new String();

		// TODO: search Semantic Web for existing one

		uri	= genRURI(value);

		return uri;
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
	private String genOURI() {
		return (new String((String) getConfig().get("namespaceOntology") + (String) getConfig().get("delimeterOntology")));
	}

	// determine and return the primary key of a table
	private String getPrimaryKeyIndex(Table table) {
		// return table.getPrimaryKeyIndex().getName();
		return "ID"; //ugly fix to circumvent jackcess bug
	}
}
