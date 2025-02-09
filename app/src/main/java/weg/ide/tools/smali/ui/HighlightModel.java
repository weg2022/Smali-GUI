package weg.ide.tools.smali.ui;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Vector;

import weg.ide.tools.smali.ui.services.CodeService;
import weg.ide.tools.smali.views.editor.EditorModel;

public class HighlightModel extends EditorModel {
	
	private final Object myStylesLock = new Object();
	private final Object mySemanticStylesLock = new Object();
	private Spans myStyles = new Spans();
	private Spans myStylesGUI = new Spans();
	private Spans mySemanticsStyles = new Spans();
	private Spans mySemanticsStylesGUI = new Spans();
	
	private final Object myErrorsLock = new Object();
	private List<CodeService.Error> myErrors = new Vector<>();
	private List<CodeService.Error> myErrorsGUI = new Vector<>();
	
	public HighlightModel() {
		super();
	}
	
	public void syntaxErrors(List<CodeService.Error> errors) {
		myErrors.clear();
		myErrors.addAll(errors);
		synchronized (myErrorsLock) {
			List<CodeService.Error> cache = myErrorsGUI;
			myErrorsGUI = myErrors;
			myErrors = cache;
		}
	}
	
	public void highlighting(int[] types, int[] starts,  int[] ends, int size) {
		myStyles.set(types, starts,ends, size);
		synchronized (myStylesLock) {
			Spans styles = myStylesGUI;
			myStylesGUI = myStyles;
			myStyles = styles;
		}
	}
	
	public void semanticHighlighting(int[] types, int[] starts, int[] ends, int size) {
		mySemanticsStyles.set(types, starts,ends, size);
		synchronized (mySemanticStylesLock) {
			Spans styles = mySemanticsStylesGUI;
			mySemanticsStylesGUI = mySemanticsStyles;
			mySemanticsStyles = styles;
		}
		//myView.invalidate();
	}
	

	
	@Override
	protected void firePrepareRemove(int offset, int length, @NonNull String removed) {
		super.firePrepareRemove(offset, length, removed);
	int end=offset+length;
		synchronized (myStylesLock) {
			myStylesGUI.remove(offset,end);
		}
		synchronized (mySemanticStylesLock) {
			mySemanticsStylesGUI.remove(offset,end);
		}
	}
	
	@Override
	protected void fireInsertUpdate(int offset, int length, @NonNull String added) {
		int end =offset+length;
		synchronized (myStylesLock) {
			myStylesGUI.insert(offset,end);
		}
		synchronized (mySemanticStylesLock) {
			mySemanticsStylesGUI.insert(offset,end);
		}
		super.fireInsertUpdate(offset, length, added);
	}
	
	
	@Override
	public boolean hasStyles() {
		return true;
	}

	
	@Override
	public int getStyle(int offset) {
		int style = mySemanticsStylesGUI.getStyle(offset);
		if (style == 0) {
			return myStylesGUI.getStyle(offset);
		}
		return style;
	}

	@Override
	public boolean isError(int line, int column) {
		line++;
		column++;
		for (CodeService.Error error : myErrorsGUI) {
			if (error.level == CodeService.Error.ERROR) {
				if (error.startLine == error.endLine && error.startLine == line) {
					return column >= error.startColumn && column <= error.endColumn;
				}
				if (error.startLine <= line && error.endLine >= line) {
					if (line == error.startLine) {
						return column >= error.startColumn;
					}
					if (line == error.endLine) {
						return column <= error.endColumn;
					}
					return true;
				}
			}
		}
		return super.isError(line, column);
	}
	
	
	public void clearErrors() {
		myErrors.clear();
		myErrorsGUI.clear();
	}
}
