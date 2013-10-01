
package arcane.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Convenience class that makes it easy to create a local database and access it in a thread safe manner.
 */
public abstract class DataStore<T extends DataStore.DataStoreConnection> {
	private final String databaseName;
	private final boolean inMemory;
	private final String tableName;
	private boolean indexesCreated;
	private List<ColumnDefinition> columns = new ArrayList<ColumnDefinition>();
	private List<String[]> indexes = new ArrayList<String[]>();
	private Connection defaultConn;
	private boolean existed;
	private ThreadLocal<T> threadConnections;

	public DataStore (String databaseName, String tableName, boolean inMemory) {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException("Database driver could not be found.", ex);
		}
		this.databaseName = databaseName;
		this.inMemory = inMemory;
		this.tableName = tableName;
	}

	/**
	 * Adds a column to the database table backing this data store. Can only be called before open is called.
	 */
	public void addColumn (String sqlColumnDefinition) {
		addColumn(new ColumnDefinition(sqlColumnDefinition));
	}

	/**
	 * Adds a column to the database table backing this data store. Can only be called before open is called.
	 */
	public void addColumn (ColumnDefinition column) {
		if (column == null) throw new IllegalArgumentException("column cannot be null.");
		if (defaultConn != null) throw new IllegalStateException("DataStore has already been opened.");
		columns.add(column);
	}

	/**
	 * Returns a List of ColumnDefinition objects.
	 */
	public List<ColumnDefinition> getColumns () {
		return columns;
	}

	/**
	 * Returns a String containing all the column names for this DataStore, delimited with commas.
	 */
	public String getColumnNames () {
		return getColumnNames(null);
	}

	/**
	 * Returns a String containing all the column names in this DataStore, delimited with commas.
	 * @param append A String to append to each column name.
	 */
	public String getColumnNames (String append) {
		StringBuffer buffer = new StringBuffer(100);
		int i = 0;
		for (Iterator<ColumnDefinition> iter = columns.iterator(); iter.hasNext(); i++) {
			ColumnDefinition column = iter.next();
			if (i > 0) buffer.append(',');
			buffer.append(column.getName());
			if (append != null) buffer.append(append);
		}
		return buffer.toString();
	}

	/**
	 * Returns a String containing a question mark for each column name in this DataStore, delimited with commas.
	 */
	public String getColumnPlaceholders () {
		StringBuffer buffer = new StringBuffer(25);
		for (int i = 0, n = columns.size(); i < n; i++) {
			if (i > 0) buffer.append(',');
			buffer.append('?');
		}
		return buffer.toString();
	}

	/**
	 * Adds an index on any number of columns.
	 */
	public void addIndex (String... columnNames) {
		if (indexesCreated) throw new IllegalStateException("Indexes have already been created.");
		indexes.add(columnNames);
	}

	/**
	 * Gets the name of the database table backing this DataStore.
	 */
	public String getTableName () {
		return tableName;
	}

	/**
	 * Returns the specified SQL with special tokens in it replaced. Eg, ":table:" is replaced with the name of the database table
	 * backing this DataStore.
	 */
	public String sql (String sql) {
		return sql.replace(":table:", getTableName());
	}

	public void open () throws SQLException {
		if (defaultConn != null) throw new IllegalStateException("DataStore has already been opened.");
		defaultConn = openConnection();
		Statement stmt = defaultConn.createStatement();

		try {
			stmt.execute(sql("SELECT 1 FROM :table:"));
			existed = true;
		} catch (SQLException ex) {
			existed = false;
		}

		StringBuffer buffer = new StringBuffer(100);
		buffer.append("CREATE TABLE IF NOT EXISTS :table: (");
		int i = 0;
		for (Iterator<ColumnDefinition> iter = columns.iterator(); iter.hasNext(); i++) {
			ColumnDefinition column = iter.next();
			if (i > 0) buffer.append(',');
			buffer.append(column.getDefinition());
		}
		buffer.append(')');
		stmt.execute(sql(buffer.toString()));
		stmt.close();

		threadConnections = new ThreadDataConnection();
	}

	/**
	 * Returns a new open connection to the database that backs all DataStores.
	 * @throws SQLException if the database connection could not be established.
	 */
	public Connection openConnection () throws SQLException {
		if (inMemory) return DriverManager.getConnection("jdbc:h2:mem:" + databaseName);
		return DriverManager.getConnection("jdbc:h2:file:" + databaseName);
		// if (inMemory) return DriverManager.getConnection("jdbc:h2:mem:" + databaseName + ";FILE_LOCK=SOCKET");
		// return DriverManager.getConnection("jdbc:h2:file:" + databaseName + ";FILE_LOCK=SOCKET");
	}

	/**
	 * Creates the indexes for the database table. Can only be called after open is called.
	 * @throws SQLException if the indexes could not be created.
	 */
	public void createIndexes () throws SQLException {
		if (defaultConn == null) throw new IllegalStateException("DataStore has not been opened.");
		if (indexesCreated) throw new IllegalStateException("Indexes have already been created.");
		if (existed) return;
		Statement stmt = defaultConn.createStatement();
		StringBuffer buffer = new StringBuffer(100);
		for (Iterator<String[]> iter = indexes.iterator(); iter.hasNext();) {
			String[] columnNames = iter.next();
			buffer.setLength(0);
			buffer.append("CREATE INDEX ix_:table:");
			for (int i = 0; i < columnNames.length; i++) {
				buffer.append('_');
				buffer.append(columnNames[i]);
			}
			buffer.append(" ON :table: (");
			for (int i = 0; i < columnNames.length; i++) {
				if (i > 0) buffer.append(',');
				buffer.append(columnNames[i]);
			}
			buffer.append(")");
			stmt.executeUpdate(sql(buffer.toString()));
		}
		stmt.close();
		indexesCreated = true;
	}

	/**
	 * Releases all resources associated with this DataStore. Afterward, all connection objects will fail if an attempt is made to
	 * use them.
	 * @throws SQLException if the close failed.
	 */
	public synchronized void close () throws SQLException {
		if (defaultConn == null) return;
		if (!defaultConn.isClosed()) defaultConn.close();
		defaultConn = null;
		threadConnections = null;
	}

	public T getThreadConnection () throws SQLException {
		if (defaultConn == null) throw new IllegalStateException("DataStore has not been opened.");
		return threadConnections.get();
	}

	abstract protected T newConnection () throws SQLException;

	/**
	 * Represents a connection to a DataStore. Each thread accessing a DataStore must do so through its own DataStoreConnection.
	 */
	public class DataStoreConnection {
		public final Connection conn;
		private PreparedStatement getCount;

		public DataStoreConnection () throws SQLException {
			conn = openConnection();
		}

		/**
		 * Conveniences method that creates a PreparedStatement using the specified SQL. Tokens in the SQL are replaced using
		 * {@link DataStore#sql(String)}.
		 * @throws SQLException if the statement could not be created.
		 */
		public PreparedStatement prepareStatement (String sql) throws SQLException {
			return conn.prepareStatement(sql(sql));
		}

		/**
		 * Releases resources associated with this connection.
		 * @throws SQLException if the close failed.
		 */
		public void close () throws SQLException {
			conn.close();
		}

		/**
		 * Empties the data store. Can only be called after open is called.
		 * @throws SQLException if the clear failed.
		 */
		public void clear () throws SQLException {
		    Statement statement = null;
		    try {
		    	statement = conn.createStatement();
		    	statement.execute(sql("DELETE FROM :table:"));
		    } finally {
			if (statement != null)
			    statement.close();
		    }
		}

		/**
		 * Returns the connection to the database for this DataStoreConnection.
		 */
		public Connection getConnection () {
			return conn;
		}

		public synchronized int getCount () throws SQLException {
			if (getCount == null) getCount = prepareStatement("SELECT COUNT(*) FROM :table:");
			ResultSet set = getCount.executeQuery();
			if (!set.next()) return 0;
			return set.getInt(1);
		}
	}

	private class ThreadDataConnection extends ThreadLocal<T> {
		protected T initialValue () {
			try {
				return newConnection();
			} catch (SQLException ex) {
				throw new RuntimeException("Unable to obtain datastore connection.", ex);
			}
		}
	}

	/**
	 * Represents a column in a SQL table. Stores the name, type, and SQL column definition.
	 */
	public class ColumnDefinition {
		private final String definition;
		private final String name;
		private final int type;

		public ColumnDefinition (String sqlColumnDefinition) {
			if (sqlColumnDefinition == null) throw new IllegalArgumentException("sqlColumnDefinition cannot be null.");

			// Determine column name and type from column definition.
			definition = sqlColumnDefinition.trim();

			int firstSpaceIndex = definition.indexOf(' ');
			if (firstSpaceIndex == -1) throw new IllegalArgumentException("Invalid column definition: " + definition);
			name = definition.substring(0, firstSpaceIndex).trim().toUpperCase();

			int firstParenIndex = definition.indexOf('(');
			if (firstParenIndex == -1) firstParenIndex = definition.length();
			String dataType = definition.substring(firstSpaceIndex + 1, firstParenIndex).trim().toUpperCase();
			if (dataType.length() == 0) throw new IllegalArgumentException("Invalid column definition: " + definition);
			if (definition.indexOf("FOR BIT DATA") != -1) dataType += " FOR BIT DATA";

			if (dataType.startsWith("BIGINT"))
				type = Types.BIGINT;
			else if (dataType.startsWith("BOOLEAN"))
				type = Types.BOOLEAN;
			else if (dataType.startsWith("BLOB"))
				type = Types.BLOB;
			else if (dataType.startsWith("CHAR FOR BIT DATA"))
				type = Types.BINARY;
			else if (dataType.startsWith("CHAR"))
				type = Types.CHAR;
			else if (dataType.startsWith("CLOB"))
				type = Types.CLOB;
			else if (dataType.startsWith("DATE"))
				type = Types.DATE;
			else if (dataType.startsWith("DECIMAL"))
				type = Types.DECIMAL;
			else if (dataType.startsWith("DOUBLE"))
				type = Types.DOUBLE;
			else if (dataType.startsWith("FLOAT"))
				type = Types.FLOAT;
			else if (dataType.startsWith("INTEGER"))
				type = Types.INTEGER;
			else if (dataType.startsWith("LONG VARCHAR FOR BIT DATA"))
				type = Types.LONGVARBINARY;
			else if (dataType.startsWith("LONG VARCHAR"))
				type = Types.LONGVARCHAR;
			else if (dataType.startsWith("NUMERIC"))
				type = Types.NUMERIC;
			else if (dataType.startsWith("REAL"))
				type = Types.REAL;
			else if (dataType.startsWith("SMALLINT"))
				type = Types.SMALLINT;
			else if (dataType.startsWith("TIME"))
				type = Types.TIME;
			else if (dataType.startsWith("TIMESTAMP"))
				type = Types.TIMESTAMP;
			else if (dataType.startsWith("VARCHAR FOR BIT DATA"))
				type = Types.VARBINARY;
			else if (dataType.startsWith("VARCHAR"))
				type = Types.VARCHAR;
			else
				throw new IllegalArgumentException("Unknown SQL column data type: " + dataType);
		}

		public String getDefinition () {
			return definition;
		}

		public String getName () {
			return name;
		}

		public int getType () {
			return type;
		}

		public String toString () {
			return getName();
		}
	}
}
