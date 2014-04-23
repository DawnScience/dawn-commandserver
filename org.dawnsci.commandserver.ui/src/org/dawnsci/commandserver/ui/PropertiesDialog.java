package org.dawnsci.commandserver.ui;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class PropertiesDialog extends Dialog {

	private Map<Object,Object> props;

	protected PropertiesDialog(Shell parentShell, Properties p) {
		super(parentShell);
		setShellStyle(SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
		this.props = new TreeMap<Object,Object>();
		props.putAll(p);
	}

	protected Control createDialogArea(Composite parent) {
		
		// create a composite with standard margins and spacing
		Composite composite = (Composite)super.createDialogArea(parent);
		
		final CLabel warning = new CLabel(composite, SWT.LEFT);
		warning.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		warning.setImage(Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/error.png").createImage());
		warning.setText("Expert queue configuration parameters, please use with caution.");
		
		TableViewer viewer   = new TableViewer(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setUseHashlookup(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createColumns(viewer);
		viewer.setContentProvider(createContentProvider());
		
		viewer.setInput(props);

		return composite;
	}

	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			
			
			private Set<Entry<Object, Object>> entries;

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				@SuppressWarnings("unchecked")
				Map<Object,Object> tmp = (Map<Object,Object>)newInput;
				if (tmp==null) return;
				this.entries = tmp.entrySet();
			}
			
			@Override
			public void dispose() {
				
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return entries.toArray(new Entry[entries.size()]);
			}
		};
	}

	private void createColumns(final TableViewer viewer) {
		
        final TableViewerColumn name = new TableViewerColumn(viewer, SWT.LEFT);
		name.getColumn().setText("Name");
		name.getColumn().setWidth(200);
		name.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return humanReadable(((Entry<?, ?>)element).getKey().toString());
			}
		});

        final TableViewerColumn value = new TableViewerColumn(viewer, SWT.LEFT);
        value.getColumn().setText("Value");
        value.getColumn().setWidth(300);
        value.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((Entry<?, ?>)element).getValue().toString();
			}
		});
        value.setEditingSupport(new EditingSupport(viewer) {

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(viewer.getTable(), SWT.NONE);
			}

			@Override
			protected boolean canEdit(Object element) {
				return element instanceof Entry<?, ?>;
			}

			@Override
			protected Object getValue(Object element) {
				Entry<Object, Object> e = (Entry<Object, Object>)element;
				return e.getValue();
			}

			@Override
			protected void setValue(Object element, Object value) {
				Entry<Object, Object> e = (Entry<Object, Object>)element;
				e.setValue(value);
				viewer.refresh(element);
			}
        	
        });
	}
	
	/**
	 * Convert Camel Case to human readable.
	 * @param s
	 * @return
	 */
	private static String humanReadable(String s) {
		String spaces = s.replaceAll(
				String.format("%s|%s|%s",
						"(?<=[A-Z])(?=[A-Z][a-z])",
						"(?<=[^A-Z])(?=[A-Z])",
						"(?<=[A-Za-z])(?=[^A-Za-z])"
						),
						" "
				);
		return spaces.substring(0,1).toUpperCase()+spaces.substring(1);
	}

	public Map<Object,Object> getProps() {
		return props;
	}

}
