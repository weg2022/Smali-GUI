package weg.ide.tools.smali.ui.services;

import java.util.List;
import java.util.Vector;

import weg.apkide.smali.org.antlr.runtime.RecognitionException;
import weg.apkide.smali.org.antlr.runtime.RecognizerSharedState;
import weg.apkide.smali.org.antlr.runtime.TokenStream;
import weg.apkide.smali.smali.SmaliParser;
import weg.ide.tools.smali.common.AppLog;

public class ExtendSmaliParser extends SmaliParser {
	
	private final List<CodeService.Error> myErrors = new Vector<>();
	
	public ExtendSmaliParser(TokenStream input) {
		super(input);
		setVerboseErrors(true);
	}
	
	public ExtendSmaliParser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	
	}
	
	synchronized public void clearErrors() {
		myErrors.clear();
	}
	
	
	@Override
	public void reportError(RecognitionException e) {
		super.reportError(e);
		AppLog.i("reportError ");
	}
	
	@Override
	public void displayRecognitionError(String[] strings, RecognitionException e) {
		AppLog.i("displayRecognitionError");
		int line = e.token.getLine();
		int column = e.token.getCharPositionInLine();
		int len = e.token == null ? 1 : e.token.getText().length();
		int endColumn = column + len;
		myErrors.add(new CodeService.Error(line, column, line, endColumn, CodeService.Error.ERROR));
		
		super.displayRecognitionError(strings, e);
	}
	
	synchronized public List<CodeService.Error> getErrors() {
		return myErrors;
	}
}
