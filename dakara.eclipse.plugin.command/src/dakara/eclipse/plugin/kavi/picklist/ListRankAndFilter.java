package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.stringscore.StringCursor;
import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class ListRankAndFilter<T> {
	private Function<InputCommand, List<T>> listContentProvider;
	private List<ColumnOptions<T>> columnOptions;
	private BiFunction<String, String, Score> rankingStrategy;
	private Function<T, String> sortFieldResolver;
	
	// TODO make generic list and filter not dependent on any UI
	public ListRankAndFilter(List<ColumnOptions<T>> columnOptions, Function<InputCommand, List<T>> listContentProvider, BiFunction<String, String, Score> rankingStrategy, Function<T, String> sortFieldResolver) {
		this.listContentProvider = listContentProvider;
		this.columnOptions = columnOptions;
		this.rankingStrategy = rankingStrategy;
		this.sortFieldResolver = sortFieldResolver;
	}
	
	public List<KaviListItem<T>> rankAndFilter(final InputCommand inputCommand) {
		return listContentProvider.apply(inputCommand).parallelStream().
				       map(item -> new KaviListItem<>(item)).
				       map(item -> setItemRank(item, inputCommand)).
				       filter(item -> item.totalScore() > 0).
				       sorted(Comparator.comparing((KaviListItem item) -> item.totalScore()).reversed().thenComparing(item -> sortFieldResolver.apply((T) item.dataItem))).
					   collect(Collectors.toList());
	}
	
	// TODO generic way to determine field filters vs inputCommand
	// map inputCommand filters to column id's (index)
//	private KaviListItem<T> setItemRank(KaviListItem<T> listItem, final InputCommand inputCommand) {
//		listItem.setScoreModeByColumn(inputCommand.isColumnFiltering);
//		
//		if (true) {
//			columnOptions.stream()	
//				.filter(options -> options.isSearchable())
//				// TODO need to change the column index - 1 which takes into account the alpha index column
//				.forEach(options -> listItem.addScore(rankingStrategy.apply(inputCommand.getColumnFilter(options.getColumnIndex() - 1), options.getColumnContentFn().apply(listItem.dataItem, -1)), options.getColumnIndex())); 
//		} else {
//			setItemRankAcrossColumns(listItem, inputCommand);
//		}
//		return listItem;
//	}
	
	private KaviListItem<T> setItemRank(KaviListItem<T> listItem, final InputCommand inputCommand) {
		listItem.setScoreModeByColumn(inputCommand.isColumnFiltering);
		
		if (inputCommand.isColumnFiltering) {
			int searchableColumnCount = 0;
			for (ColumnOptions<T> options : columnOptions) {
				if (!options.isSearchable()) continue;
				listItem.addScore(rankingStrategy.apply(inputCommand.getColumnFilter(searchableColumnCount), options.getColumnContentFn().apply(listItem.dataItem, -1)), options.getColumnIndex());
				searchableColumnCount++;
			} 
		} else {
			setItemRankAcrossColumns(listItem, inputCommand);
		}
		return listItem;
	}
	
	private KaviListItem<T> setItemRankAcrossColumns(KaviListItem<T> listItem, final InputCommand inputCommand) {
		List<Integer> indexesOfColumnBreaks = new ArrayList<>();
		StringBuilder allColumnText = new StringBuilder();
		buildAllColumnTextAndIndexes(listItem, indexesOfColumnBreaks, allColumnText);
		
		Score allColumnScore = rankingStrategy.apply( inputCommand.getColumnFilter(0), allColumnText.toString());
		if (allColumnScore.rank > 0) {
			System.out.println(new StringCursor(allColumnText.toString()).setMarkers(allColumnScore.matches).markersAsString());
			List<Score> columnScores = splitScoreByColumns(allColumnText.toString(), allColumnScore, indexesOfColumnBreaks);
			int columnIndex = 1;
			for (Score score : columnScores) {
				listItem.addScore(score, columnIndex++);
			} 
		} else {
			listItem.addScore(allColumnScore, 1);
		}
		return listItem;
	}

	private void buildAllColumnTextAndIndexes(KaviListItem<T> listItem, List<Integer> indexesOfColumnBreaks, StringBuilder allColumnText) {
		for (ColumnOptions<T> column : columnOptions) {
			if (!column.isSearchable()) continue;
			
			String columnContent = column.getColumnContentFn().apply(listItem.dataItem, -1);
			allColumnText.append(columnContent).append(" ");
			indexesOfColumnBreaks.add(allColumnText.length() - 1);
		}
	}	
	
	private List<Score> splitScoreByColumns(String originalText, Score allColumnScore, List<Integer> indexesOfColumnBreaks) {
		List<Score> scores = new ArrayList<>();
		List<Integer> matches = new ArrayList<>();
		int offset = 0;
		
		for (int index = 0; index < originalText.length(); index++) {
			if (allColumnScore.matches.size() > 0 && index == allColumnScore.matches.get(0)) {
				allColumnScore.matches.remove(0);
				matches.add(index - offset);
			}
			
			if (index == indexesOfColumnBreaks.get(0) || index == originalText.length() - 1) {
				indexesOfColumnBreaks.remove(0);
				scores.add(new Score(allColumnScore.rank, matches));
				matches = new ArrayList<>();
				offset += index + 1;
			}
		}
		
		return scores;
	}
}
