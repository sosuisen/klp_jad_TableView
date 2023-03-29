package com.example;

import java.time.LocalDate;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

public class MyAppController {
	private ArrayList<ToDo> todos = new ArrayList<>() {
		{
			add(new ToDo(0, "Design", LocalDate.parse("2022-12-01"), true));
			add(new ToDo(1, "Implementation", LocalDate.parse("2022-12-07"), false));
		}
	};

	// TableViewの型パラメータはデータクラス
	@FXML
	private TableView<ToDo> tableView;

	/**
	 * TableViewの列は、TableColumn型のオブジェクトで管理されます。
	 * それぞれ型パラメータとして<データクラス, 列に対応するデータ型>を用います。
	 */
	@FXML
	private TableColumn<ToDo, Boolean> completedCol;

	@FXML
	private TableColumn<ToDo, String> titleCol;

	@FXML
	private TableColumn<ToDo, LocalDate> dateCol;

	@FXML
	private TableColumn<ToDo, ToDo> deleteCol;

	@FXML
	private Button addBtn;

	@FXML
	private DatePicker datePicker;

	@FXML
	private TextField titleField;

	@FXML
	private MenuItem menuItemAbout;

	@FXML
	private MenuItem menuItemClose;

	// TableViewのObservableListの型パラメータは、データクラスToDoを用います。
	private ObservableList<ToDo> tableViewItems;

	public ToDo create(String title, LocalDate date) {
		int newId;
		if (todos.size() > 0)
			newId = todos.stream().max((todo1, todo2) -> todo1.getId() - todo2.getId()).get().getId() + 1;
		else
			newId = 0;
		var newToDo = new ToDo(newId, title, date, false);
		todos.add(newToDo);

		return newToDo;
	}

	private void showInfo(String txt) {
		Alert dialog = new Alert(AlertType.INFORMATION);
		dialog.setTitle("アプリの情報");
		dialog.setHeaderText(null);
		dialog.setContentText(txt);
		dialog.showAndWait();
	}

	public void initialize() {
		// 入力欄の日付を今日に設定
		datePicker.setValue(LocalDate.now());

		/**
		 *  TableViewを設定
		 */
		// ObservableListを取得
		tableViewItems = tableView.getItems();

		// セル値ファクトリ：
		// setCellValueFactoryで列ごとのセル値ファクトリを設定します。
		// セル値ファクトリは、セルと関連するプロパティを返すラムダ式です。
		// ToDoクラスのフィールドを表示するセルについては、
		// new PropertyValueFactory<>(ToDoクラスのフィールド名)と書くと
		// 自動的にセル値ファクトリが生成されます。
		completedCol.setCellValueFactory(new PropertyValueFactory<>("completed"));
		titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
		// 削除ボタンのように、ToDoクラスのフィールドと関係ないセルにおいて、
		// 次のラムダ式をセル値ファクトリとすることで、該当セルに対応するToDoオブジェクトを
		// セルファクトリのほうへ渡すことができます。
		// ここでは、p.getValue() がToDoオブジェクトを返しています。
		deleteCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue()));

		// セルファクトリ：
		// setCellFactoryで、列ごとのセルファクトリを設定します。
		// セルファクトリは、セルを生成して返すラムダ式です。
		// セルを構成するGUI部品に対応するTableCellクラス（CheckBoxTableCellなど）の
		// forTableColumnメソッドで自動的にセルファクトリが生成されます。
		// が、対応するTableCellクラスがないセルは、自作する必要があります（今回はdateColとdeleteCol）。
		completedCol.setCellFactory(CheckBoxTableCell.forTableColumn(completedCol));
		titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
		// セルファクトリとなるLocalDateTableCellクラスを自作。LocalDateTableCell.java参照
		dateCol.setCellFactory(LocalDateTableCell<ToDo>::new);
		// セルファクトリのラムダ式を自作
		deleteCol.setCellFactory(param -> new TableCell<ToDo, ToDo>() {
			private final Button deleteBtn = new Button("🗑");
			// セル値ファクトリで、このセルに対応するToDoオブジェクトが紐付けられています。
			// updateItemの第1引数で受け取ることができます。
			protected void updateItem(ToDo todo, boolean empty) {
				super.updateItem(todo, empty);
				if (todo == null) {
					setGraphic(null);
					return;
				}
				// このセルに表示する内容を定めます。
				setGraphic(deleteBtn); // 削除ボタン
				deleteBtn.setPrefWidth(35);
				// 削除ボタンが押されたときのイベントハンドラ
				deleteBtn.setOnAction(
						e -> {
							todos.remove(todo);
							getTableView().getItems().remove(todo);
						});
			}
		});

		// デフォルトのソート・キー（ソートの基準となるTableColumn）として日付列を定めます。
		tableView.getSortOrder().add(dateCol);

		// getSortOrder()で得られるソート順ObservableListは、1つ以上のソート・キーを格納します。
		// （複数キーでのソートが可能です）
		// ソートは、TableViewの列のタイトル部をクリックするごとに、「昇順」「降順」「ソートなし」の3状態へ順に変化します。
		// 「ソートなし」の状態をスキップして「昇順」「降順」のみにしたい場合、
		// ソート順ObservableList内のソート・キーが「ソートなし」になったことを検出したときに、
		// 強制的に「昇順」のソート・キーををセットする必要があります。
		// See also https://stackoverflow.com/questions/52567754/javafx-tableview-column-sorting-has-three-states-why
		tableView.getSortOrder().addListener((ListChangeListener.Change<? extends TableColumn<ToDo, ?>> change) -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					// ソート順ObservableListからソート・キーのTableColumnが削除されると
					// wasRemoved()がtrueを返します。
					// これが「ソートなし」の状態です。
					// 削除された TableColumnをまず取得。
					var removedSortCol = change.getRemoved().get(0);
					if (change.getList().size() == 0) {
						Platform.runLater(() -> {
							// 削除されたTableColumnを昇順にセット
							removedSortCol.setSortType(SortType.ASCENDING);
							// 削除されたTableColumnをソート順ObservableListへ追加
							tableView.getSortOrder().add(removedSortCol);
						});
					}
				}
			}
		});

		tableViewItems.addAll(todos);

		EventHandler<ActionEvent> handler = e -> {
			var title = titleField.getText();
			if (title.equals(""))
				return;
			LocalDate localDate = datePicker.getValue(); // 2022-12-01
			ToDo newToDo = create(title, localDate);
			tableViewItems.add(newToDo);
			titleField.setText("");
			tableView.sort();
		};
		titleField.setOnAction(handler);
		addBtn.setOnAction(handler);

		menuItemAbout.setOnAction(e -> showInfo("ToDo App"));
		menuItemClose.setOnAction(e -> Platform.exit());
	}
}
