package weg.ide.tools.smali.ui;

import static weg.ide.tools.smali.ui.App.init;
import static weg.ide.tools.smali.ui.App.postExec;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import weg.ide.tools.smali.common.IoUtils;
import weg.ide.tools.smali.common.KeyStrokeDetector;
import weg.ide.tools.smali.common.LineEndingNormalizedReader;
import weg.ide.tools.smali.common.MessageBox;
import weg.ide.tools.smali.ui.services.CodeService;
import weg.ide.tools.smali.views.EditorView;
import weg.ide.tools.smali.views.editor.CaretListener;
import weg.ide.tools.smali.views.editor.Editor;
import weg.ide.tools.smali.views.editor.SelectionListener;

public class MainUI extends Activity implements TextModel.TextModelListener,
		CaretListener,
		SelectionListener,
		CodeService.ErrorListener,
		CodeService.HighlightingListener{
	
	
	private KeyStrokeDetector myKeyStrokeDetector;
	private EditorView myEditorView;
	private HighlightModel myModel;
	private QuickKeyBar myQuickKeyBar;
	private long myLastPressed = -1;
	
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init(this);
		myKeyStrokeDetector = new KeyStrokeDetector(this);
		setContentView(R.layout.main);
		myEditorView = findViewById(R.id.editorView);
		App.getCodeService().load();
		MessageBox.queryFromList(this, "What's new",
				List.of("代码/语义高亮",
						"实时错误检查与类型检查(仅Android API)",
						"无限次撤销与重做"
				), value -> {
			
		});
		myQuickKeyBar = new QuickKeyBar(this);
		myQuickKeyBar.setKeys(myEditorView.getQuickKeys());
		myQuickKeyBar.showQuickKeyBar(true);
		myEditorView.setKeyStrokeDetector(myKeyStrokeDetector);
		myEditorView.setCaretVisible(true);
		myEditorView.setInsertTabsAsSpaces(false);
		myEditorView.setIndentationEnabled(true);
		//myEditorView.setWhitespaceVisible(true);
		myModel = new HighlightModel();
		App.getCodeService().setErrorListener(this);
		App.getCodeService().setHighlightingListener(this);
		postExec(() -> {
			myModel.setUndoEnabled(false);
			try {
				String buffer = IoUtils.readString(new LineEndingNormalizedReader(new InputStreamReader(getAssets().open("Main.smali"))), true);
				myModel.insert(0, buffer);
			} catch (IOException e) {
				myModel.insert(0, Objects.requireNonNull(e.getLocalizedMessage()));
			}
			myModel.setUndoEnabled(true);
			
			runOnUiThread(() -> {
				myEditorView.setModel(myModel);
				myEditorView.addModelListener(MainUI.this);
				myEditorView.addCaretListener(MainUI.this);
				myEditorView.addSelectionListener(MainUI.this);
				
				App.getCodeService().highlightSmali(myModel);
			});
		});
		if (getActionBar() != null)
			getActionBar().setDisplayHomeAsUpEnabled(false);
	}
	
	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - myLastPressed <= 2000) {
			super.onBackPressed();
			return;
		}
		myLastPressed = System.currentTimeMillis();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		App.shutdown();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (myEditorView.isSelectionMode()) {
			getMenuInflater().inflate(R.menu.editor_menu, menu);
			return true;
		}
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		
		if (item.getItemId() == R.id.commandCopy) {
			if (myEditorView.canCopy())
				myEditorView.copy();
			return true;
		} else if (item.getItemId() == R.id.commandCut) {
			if (myEditorView.canCut())
				myEditorView.cut();
			return true;
		} else if (item.getItemId() == R.id.commandPaste) {
			if (myEditorView.canPaste())
				myEditorView.paste();
			return true;
		} else if (item.getItemId() == R.id.commandSelectAll) {
			if (myEditorView.canSelectAll())
				myEditorView.selectAll();
			return true;
		} else if (item.getItemId() == R.id.commandUnSelect) {
			if (myEditorView.canUnselectAll())
				myEditorView.unselectAll();
			return true;
		} else if (item.getItemId() == R.id.commandUndo) {
			if (myEditorView.canUndo())
				myEditorView.undo();
			return true;
		} else if (item.getItemId() == R.id.commandRedo) {
			if (myEditorView.canRedo())
				myEditorView.redo();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		myKeyStrokeDetector.configChange(this);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		myKeyStrokeDetector.activityKeyUp(keyCode, event);
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		myKeyStrokeDetector.activityKeyDown(keyCode, event);
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void insertUpdate(@NonNull TextModel textModel, int offset, int length, @NonNull String added) {
		App.getCodeService().highlightSmali(myModel);
		if (getActionBar() != null) {
			getActionBar().setSubtitle("tc:"+textModel.getCharCount() +" tl:"+textModel.getLineCount());
		}
		invalidateOptionsMenu();
	}
	
	@Override
	public void removeUpdate(@NonNull TextModel textModel, int offset, int length, @NonNull String removed) {
		App.getCodeService().highlightSmali(myModel);
		if (getActionBar() != null) {
			getActionBar().setSubtitle("tc:"+textModel.getCharCount() +" tl:"+textModel.getLineCount());
			
		}
		invalidateOptionsMenu();
	}
	
	@Override
	public void caretUpdate(@NonNull Editor view, int caretOffset, boolean typing) {
		
		invalidateOptionsMenu();
	}
	
	@Override
	public void selectionUpdate(@NonNull Editor view, boolean selectMode, int selectStart, int selectEnd) {
		if (getActionBar() != null) {
			getActionBar().setDisplayShowTitleEnabled(!selectMode);
			
		}
		
		invalidateOptionsMenu();
	}
	
	@Override
	public void syntaxErrors(List<CodeService.Error> errors) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				
				myModel.syntaxErrors(errors);
			}
		});
		
	}
	
	public QuickKeyBar getQuickKeyBar() {
		return myQuickKeyBar;
	}
	
	@Override
	public void highlighting(int[] styles, int[] starts, int[] ends, int size) {
		myModel.highlighting(styles, starts, ends, size);
	}
	
	@Override
	public void semanticHighlighting(int[] styles, int[] starts, int[] ends, int size) {
		myModel.semanticHighlighting(styles, starts, ends, size);
	}

}
