package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

import dakara.eclipse.plugin.stringscore.RankedItem;

public class KaviListColumns<T> {
	private final List<ColumnOptions<T>> columnOptions = new ArrayList<>();
	private final TableViewer tableViewer;
	
	public KaviListColumns(TableViewer tableViewer) {
		this.tableViewer = tableViewer;
	}
	
	public ColumnOptions<T> addColumn(String columnId, Function<T, String> columnContentFn) {
		return addColumn(columnId, (item, rowIndex) -> columnContentFn.apply(item));
	}
	
	public ColumnOptions<T> addColumn(String columnId, BiFunction<T, Integer, String> columnContentFn) {
		final ColumnOptions<T> options = new ColumnOptions<T>(columnId, columnContentFn, columnOptions.size());
		StyledCellLabelProvider labelProvider = new StyledCellLabelProvider(StyledCellLabelProvider.COLORS_ON_SELECTION) {
			@Override
        	public void update(ViewerCell cell) {
        		// TODO reuse and manage SWT resources
        		final RankedItem<T> rankedItem = applyCellDefaultStyles(options, cell);
        		resolveCellTextValue(columnContentFn, cell, rankedItem);
        		if (options.isSearchable())
        			applyCellScoreMatchStyles(cell, rankedItem);
                super.update(cell);
        	}
		};
		options.setColumn(createTableViewerColumn(tableViewer, labelProvider).getColumn());
		columnOptions.add(options);
		return options;
	}
	
	public List<ColumnOptions<T>> getColumnOptions() {
		return columnOptions;
	}
	
    private TableViewerColumn createTableViewerColumn(TableViewer tableViewer, StyledCellLabelProvider labelProvider) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        viewerColumn.setLabelProvider(labelProvider);
        return viewerColumn;
    }	
    
	
	@SuppressWarnings("unchecked")
	private RankedItem<T> applyCellDefaultStyles(final ColumnOptions<T> options, ViewerCell cell) {
		cell.setForeground(fromRegistry(options.getFontColor()));
		cell.setBackground(fromRegistry(options.getBackgroundColor()));
		Font font = createColumnFont(options, cell);
		cell.setFont(font);
		final RankedItem<T> rankedItem = (RankedItem<T>) cell.getElement();
		return rankedItem;
	}

	private Font createColumnFont(final ColumnOptions<T> options, ViewerCell cell) {
		Font font = options.getFont();
		if (font == null) {
			FontDescriptor fontDescriptor = FontDescriptor.createFrom(cell.getFont()).setStyle(options.getFontStyle());
			font = fontDescriptor.createFont(cell.getControl().getDisplay());
			options.setFont(font);
		}
		return font;
	}
	
	private Color fromRegistry(RGB rgb) {
		String symbolicName = rgb.red + "." + rgb.blue + "." + rgb.green;
		Color color = JFaceResources.getColorRegistry().get(symbolicName);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
		}
		return color;
	}
	
	private void resolveCellTextValue(BiFunction<T, Integer, String> columnContentFn, ViewerCell cell, final RankedItem<T> rankedItem) {
		cell.setText(columnContentFn.apply(rankedItem.dataItem, tableViewer.getTable().indexOf((TableItem) cell.getItem())));
	}	
	private void applyCellScoreMatchStyles(ViewerCell cell, final RankedItem<T> rankedItem) {
		cell.setStyleRanges(createStyles(rankedItem.getColumnScore(getColumnIdFromColumnIndex(cell.getColumnIndex())).matches));
	}
	
	private String getColumnIdFromColumnIndex(int columnIndex) {
		// TODO consider: likely there will never be lots of columns.  Looping is probably optimal here vs a lookup table.
		for (ColumnOptions options : columnOptions) {
			if (options.columnIndex == columnIndex) return options.columnId;
		}
		throw new IllegalStateException("No matching column index");
	}
	
    private StyleRange[] createStyles(List<Integer> matches) {
    	List<StyleRange> styles = new ArrayList<StyleRange>();
    	for (Integer match : matches) {
    		styles.add(new StyleRange(match, 1, null, fromRegistry(new RGB(150,190,255))));
    	}
    	return styles.toArray(new StyleRange[]{});
    }
}
