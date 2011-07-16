
package arcane.ui.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import arcane.Arcane;
import arcane.ArcaneException;
import arcane.Card;
import arcane.CardProperty;
import arcane.ui.util.CheckMenuItem;

public class CardTable extends SortedTable {
	public Set<Card> illegalCards = new HashSet<Card>();

	private JCheckBoxMenuItem autoSizeColumnsMenuItem;
	private JMenu columnsMenu;
	private Set<CardProperty> hiddenProperties;
	private boolean ignoreCopyEvents;

	public CardTable () {
		this(false, (CardProperty[])null);
	}

	public CardTable (boolean editable, CardProperty... hiddenProperties) {
		this.hiddenProperties = new HashSet<CardProperty>();
		if (hiddenProperties != null) this.hiddenProperties.addAll(Arrays.asList(hiddenProperties));

		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		setDefaultRenderer(Object.class, new PaddedRenderer(1, 2));
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		setBackground(UIManager.getColor("Table.background"));
		// setBorder(BorderFactory.createEmptyBorder());

		addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent evt) {
				if (evt.getButton() == 1) return;
				int rowIndex = rowAtPoint(evt.getPoint());
				for (int selectedIndex : getSelectedRows())
					if (selectedIndex == rowIndex) return;
				getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
			}
		});

		setupContextMenu();

		model = new CardTableModel(editable);
		setModel(model);
	}

	public void copyTable (final CardTable copyTable) {
		copyTable.model.addTableModelListener(new TableModelListener() {
			public void tableChanged (TableModelEvent evt) {
				if (ignoreCopyEvents) return;
				if (evt.getFirstRow() != TableModelEvent.HEADER_ROW) return;
				copyTable.ignoreCopyEvents = true;
				model.setProperties(copyTable.model.properties);
				copyTable.ignoreCopyEvents = false;
			}
		});

		copyTable.columnModel.addColumnModelListener(new TableColumnModelListener() {
			public void columnAdded (TableColumnModelEvent evt) {
			}

			public void columnMarginChanged (ChangeEvent evt) {
				if (ignoreCopyEvents) return;
				copyTable.ignoreCopyEvents = true;
				for (int i = 0, n = copyTable.columnModel.getColumnCount(); i < n; i++)
					columnModel.getColumn(i).setPreferredWidth(copyTable.columnModel.getColumn(i).getPreferredWidth());
				copyTable.ignoreCopyEvents = false;
			}

			public void columnMoved (TableColumnModelEvent evt) {
				if (ignoreCopyEvents) return;
				if (evt.getFromIndex() == evt.getToIndex()) return;
				copyTable.ignoreCopyEvents = true;
				columnModel.moveColumn(evt.getFromIndex(), evt.getToIndex());
				copyTable.ignoreCopyEvents = false;
			}

			public void columnRemoved (TableColumnModelEvent evt) {
			}

			public void columnSelectionChanged (ListSelectionEvent evt) {
			}
		});

		setAutoResizeMode(copyTable.getAutoResizeMode());
		copyTable.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange (PropertyChangeEvent evt) {
				if (ignoreCopyEvents) return;
				if (!evt.getPropertyName().equals("autoResizeMode")) return;
				copyTable.ignoreCopyEvents = true;
				setAutoResizeMode((Integer)evt.getNewValue());
				copyTable.ignoreCopyEvents = false;
			}
		});

		JScrollPane scrollPane = (JScrollPane)copyTable.getParent().getParent();
		final JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
		horizontalScrollBar.getModel().addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent evt) {
				if (ignoreCopyEvents) return;
				copyTable.ignoreCopyEvents = true;
				((JScrollPane)getParent().getParent()).getHorizontalScrollBar().setValue(horizontalScrollBar.getValue());
				copyTable.ignoreCopyEvents = false;
			}
		});

		copyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent evt) {
				if (evt.getValueIsAdjusting()) return;
				if (copyTable.getSelectedRow() != -1) clearSelection();
			}
		});
	}

	public void tableChanged (TableModelEvent evt) {
		super.tableChanged(evt);
		if (model == null) return;
		if (evt.getFirstRow() != TableModelEvent.HEADER_ROW) return;

		// Set any special renders when the table structure changes.
		int i = 0;
		for (final CardProperty property : model.properties) {
			switch (property) {
			case price:
				columnModel.getColumn(i).setCellRenderer(new PaddedRenderer(1, 2) {
					public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
						String price = "$" + value;
						if (price.length() > 2 && price.charAt(price.length() - 2) == '.') price += "0";
						return super.getTableCellRendererComponent(table, price, isSelected, hasFocus, row, column);
					}
				});
				break;
			case rating:
				columnModel.getColumn(i).setCellRenderer(new RatingRenderer(this, i));
				break;
			case flags:
				columnModel.getColumn(i).setCellRenderer(new FlagRenderer(this, i));
				break;
			case ownedQty: {
				final JTextField textField = new JTextField();
				// textField.setBackground(getSelectionBackground());
				// textField.setCaretColor(getSelectionForeground());
				// textField.setForeground(getSelectionForeground());
				// textField.setBorder(BorderFactory.createMatteBorder(2, 3, 2, 2, getSelectionBackground()));
				textField.addMouseListener(new MouseAdapter() {
					public void mouseClicked (MouseEvent evt) {
						if (evt.getClickCount() < 2 || evt.getClickCount() % 2 != 0) return;
						int ownedQty = 0;
						String value = textField.getText().replaceAll("[^0-9]", "");
						if (value.length() == 0) return;
						ownedQty = Integer.parseInt(value);
						for (int i = 0, n = evt.isAltDown() ? 4 : 1; i < n; i++) {
							if (evt.getButton() == 3) {
								ownedQty--;
								if (ownedQty < 0) ownedQty = 0;
							} else
								ownedQty++;
						}
						textField.setText(String.valueOf(ownedQty));
					}
				});
				final DefaultCellEditor textCellEditor = new DefaultCellEditor(textField) {
					private Card editingCard;

					public Component getTableCellEditorComponent (JTable table, Object value, boolean isSelected, int row, int column) {
						CardTable cardTable = (CardTable)table;
						editingCard = cardTable.model.viewCards.get(row);
						textField.setText(String.valueOf(editingCard.ownedQty));
						return super.getTableCellEditorComponent(table, value, isSelected, row, column);
					}

					public Object getCellEditorValue () {
						int ownedQty = 0;
						String value = textField.getText().replaceAll("[^0-9]", "");
						if (value.length() > 0) ownedQty = Integer.parseInt(value);
						editingCard.ownedQty = ownedQty;
						// BOZO
						// DeckBuilder.updateRows(editingCard.name);
						return ownedQty;
					}
				};
				// textCellEditor.setClickCountToStart(1);
				columnModel.getColumn(i).setCellEditor(textCellEditor);
				break;
			}
			case castingCost:
			case manaProduced:
				columnModel.getColumn(i).setCellRenderer(new ManaSymbolRenderer());
				break;
			case power:
			case touhgness:
				// Hide if pt is empty.
				columnModel.getColumn(i).setCellRenderer(new PaddedRenderer(1, 2) {
					public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
						if (model.viewCards.get(row).pt.length() == 0) value = "";
						return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					}
				});
				break;
			case set:
			case setName: {
				final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
				JComboBox comboBox = new JComboBox(comboBoxModel);
				columnModel.getColumn(i).setCellEditor(new DefaultCellEditor(comboBox) {
					private Card editingCard;

					public Component getTableCellEditorComponent (JTable table, Object value, boolean isSelected, int row, int column) {
						comboBoxModel.removeAllElements();
						CardTable cardTable = (CardTable)table;
						editingCard = cardTable.model.viewCards.get(row);
						for (String set : Arcane.getInstance().getSets(editingCard.name)) {
							if (property == CardProperty.set)
								comboBoxModel.addElement(set.toUpperCase());
							else
								comboBoxModel.addElement(Arcane.getInstance().getSetName(set));
						}
						comboBoxModel.setSelectedItem(editingCard.set);
						return super.getTableCellEditorComponent(table, value, isSelected, row, column);
					}

					public Object getCellEditorValue () {
						String selectedItem = ((String)comboBoxModel.getSelectedItem());
						String set = Arcane.getInstance().getMainSet(selectedItem);
						if(editingCard.set.equals(set))
							return set;
						Set<String> versions = Arcane.getInstance().getVersions(editingCard.name, set);
						String version;
						if(versions.contains(editingCard.version))
							version = editingCard.version;
						else
							version = (String)(versions.toArray()[0]);
						model.unsortedCards.remove(editingCard);
						model.unsortedCards.add(Arcane.getInstance().getCard(editingCard.name, set, version));
						sort();
						return set;
					}
				});
				break;
			}
			case version: {
				// Hide zeros.
				/*columnModel.getColumn(i).setCellRenderer(new PaddedRenderer(1, 2) {
					public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
						if ((Integer)value == 0) value = "";
						return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					}
				});*/

				final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
				JComboBox comboBox = new JComboBox(comboBoxModel);
				columnModel.getColumn(i).setCellEditor(new DefaultCellEditor(comboBox) {
					private Card editingCard;

					public Component getTableCellEditorComponent (JTable table, Object value, boolean isSelected, int row, int column) {
						comboBoxModel.removeAllElements();
						CardTable cardTable = (CardTable)table;
						editingCard = cardTable.model.viewCards.get(row);

						Set<String> versions = Arcane.getInstance().getVersions(editingCard.englishName, editingCard.set);
						for (String version : versions) {
							if ("".equals(version))
								comboBoxModel.addElement("");
							else
								comboBoxModel.addElement(version);
						}

						if ("".equals(editingCard.version))
							comboBoxModel.setSelectedItem("");
						else
							comboBoxModel.setSelectedItem(editingCard.version);
						return super.getTableCellEditorComponent(table, value, isSelected, row, column);
					}

					public Object getCellEditorValue () {
						String selectedVersion;
						if ("".equals(comboBoxModel.getSelectedItem()))
							selectedVersion = "";
						else
							selectedVersion = (String)comboBoxModel.getSelectedItem();
						model.unsortedCards.remove(editingCard);
						model.unsortedCards.add(Arcane.getInstance().getCard(editingCard.name, editingCard.set, selectedVersion));
						sort();
						return selectedVersion;
					}
				});
				break;
			}
			}
			i++;
		}

		// Set the selected property for each context menu checkbox.
		outerLoop: for (Component component : columnsMenu.getMenuComponents()) {
			if (!(component instanceof CheckMenuItem)) continue;
			CheckMenuItem menuItem = (CheckMenuItem)component;
			for (CardProperty property : model.properties) {
				if (property.text.equals(menuItem.getText())) {
					menuItem.setSelected(true);
					continue outerLoop;
				}
			}
			menuItem.setSelected(false);
		}
	}

	private void setupContextMenu () {
		final JPopupMenu contextMenu = new JPopupMenu();
		getTableHeader().addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent evt) {
				if (evt.getButton() == 3) contextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
			}
		});

		columnsMenu = new JMenu("Show Columns");
		contextMenu.add(columnsMenu);
		ActionListener checkListener = new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				CheckMenuItem menuItem = (CheckMenuItem)evt.getSource();
				// Don't allow last column to be removed.
				if (!menuItem.isSelected() && model.properties.length == 1) {
					menuItem.setSelected(true);
					return;
				}
				// Store column widths.
				int count = columnModel.getColumnCount();
				List<Integer> widths = new ArrayList<Integer>();
				for (int i = 0; i < count; i++)
					widths.add(columnModel.getColumn(i).getPreferredWidth());
				// Get Property associated with the menu item.
				CardProperty selectedProperty = null;
				for (CardProperty property : CardProperty.values()) {
					if (property.text.equals(menuItem.getText())) {
						selectedProperty = property;
						break;
					}
				}
				// Set new properties.
				List<CardProperty> properties = new ArrayList<CardProperty>(Arrays.asList(model.properties));
				if (menuItem.isSelected())
					properties.add(selectedProperty);
				else {
					int columnIndex = properties.indexOf(selectedProperty);
					widths.remove(columnIndex);
					properties.remove(columnIndex);
				}
				model.setProperties(properties.toArray(new CardProperty[0]));
				// Restore column widths.
				for (int i = 0, n = widths.size(); i < n; i++)
					columnModel.getColumn(i).setPreferredWidth(widths.get(i));
			}
		};
		for (CardProperty property : CardProperty.values()) {
			if (hiddenProperties != null && hiddenProperties.contains(property)) continue;
			CheckMenuItem menuItem = new CheckMenuItem(property.text);
			menuItem.addActionListener(checkListener);
			columnsMenu.add(menuItem);
		}
		columnsMenu.add(new Separator());
		columnsMenu.add(new JMenuItem("Close"));

		contextMenu.add(autoSizeColumnsMenuItem = new JCheckBoxMenuItem("Auto Size Columns"));
		autoSizeColumnsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent evt) {
				setAutoResizeMode(autoSizeColumnsMenuItem.isSelected() ? AUTO_RESIZE_SUBSEQUENT_COLUMNS : AUTO_RESIZE_OFF);
			}
		});
	}

	public int getQty (Card card) {
		Integer qty = model.cardToQty.get(card);
		if (qty == null) return 0;
		return qty;
	}

	public Map<String, String> getPreferences () {
		Map<String, String> values = new HashMap<String, String>();
		StringBuffer buffer = new StringBuffer(256);

		int count = columnModel.getColumnCount();
		for (int i = 0; i < count; i++) {
			buffer.append(model.properties[columnModel.getColumn(i).getModelIndex()]);
			buffer.append(',');
		}
		buffer.setLength(buffer.length() - 1);
		values.put("columns", buffer.toString());

		buffer.setLength(0);
		for (int i = 0; i < count; i++) {
			buffer.append(columnModel.getColumn(i).getPreferredWidth());
			buffer.append(',');
		}
		buffer.setLength(buffer.length() - 1);
		values.put("widths", buffer.toString());
		buffer.setLength(0);

		values.put("auto.resize", String.valueOf(getAutoResizeMode()));

		buffer.setLength(0);
		// A sorted column could have been removed.
		for (Iterator<SortedColumn> iter = sortedColumns.iterator(); iter.hasNext();)
			if (iter.next().index >= model.properties.length) iter.remove();
		for (SortedColumn sortedColumn : sortedColumns) {
			buffer.append(convertColumnIndexToModel(sortedColumn.index));
			buffer.append(',');
			buffer.append(sortedColumn.direction.toString());
			buffer.append(';');
		}
		if (buffer.length() > 0) buffer.setLength(buffer.length() - 1);
		values.put("sort", buffer.toString());

		return values;
	}

	public void setPreferences (Map<String, String> values) {
		try {
			String columns = values.get("columns");
			if (columns != null) {
				List<CardProperty> properties = new ArrayList<CardProperty>();
				for (String propertyName : columns.split(","))
					properties.add(CardProperty.valueOf(propertyName));
				model.setProperties(properties.toArray(new CardProperty[0]));
			}

			String widths = values.get("widths");
			if (widths != null) {
				int i = 0;
				for (String width : widths.split(","))
					columnModel.getColumn(i++).setPreferredWidth(Integer.parseInt(width));
			}

			String autoResize = values.get("auto.resize");
			if (autoResize != null) setAutoResizeMode(Integer.parseInt(autoResize));

			String sort = values.get("sort");
			if (sort != null && sort.length() > 0) {
				for (String sortString : sort.split(";")) {
					String[] sortArray = sortString.split(",");
					setSortDirection(Integer.parseInt(sortArray[0]), Direction.valueOf(sortArray[1]));
				}
			}
		} catch (RuntimeException ex) {
			throw new ArcaneException("Error setting card table preferences.", ex);
		}
	}

	public void setAutoResizeMode (int mode) {
		super.setAutoResizeMode(mode);
		if (autoSizeColumnsMenuItem != null) autoSizeColumnsMenuItem.setSelected(mode != AUTO_RESIZE_OFF);
	}
}
