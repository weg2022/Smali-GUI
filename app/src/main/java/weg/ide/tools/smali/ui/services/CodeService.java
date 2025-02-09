package weg.ide.tools.smali.ui.services;

import static java.io.File.separator;
import static java.io.File.separatorChar;
import static weg.apkide.smali.smali.SmaliParser.*;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import weg.apkide.smali.dexlib2.Opcodes;
import weg.apkide.smali.dexlib2.writer.builder.DexBuilder;
import weg.apkide.smali.org.antlr.runtime.CommonToken;
import weg.apkide.smali.org.antlr.runtime.CommonTokenStream;
import weg.apkide.smali.org.antlr.runtime.tree.CommonTree;
import weg.apkide.smali.org.antlr.runtime.tree.CommonTreeNodeStream;
import weg.apkide.smali.org.antlr.runtime.tree.TreeVisitor;
import weg.apkide.smali.org.antlr.runtime.tree.TreeVisitorAction;
import weg.apkide.smali.smali.InvalidToken;
import weg.apkide.smali.smali.SmaliFlexLexer;
import weg.ide.tools.smali.common.AppLog;
import weg.ide.tools.smali.common.HighlightSpace;
import weg.ide.tools.smali.common.IoUtils;
import weg.ide.tools.smali.ui.App;
import weg.ide.tools.smali.ui.HighlightModel;

public class CodeService {
	public static final int PlainStyle = 0;
	public static final int KeywordStyle = 1;
	public static final int OperatorStyle = 2;
	public static final int SeparatorStyle = 3;
	public static final int StringStyle = 4;
	public static final int NumberStyle = 5;
	public static final int MetadataStyle = 6;
	public static final int IdentifierStyle = 7;
	public static final int NamespaceStyle = 8;
	public static final int TypeStyle = 9;
	public static final int FieldStyle = 10;
	public static final int VariableStyle = 11;
	public static final int FunctionStyle = 12;
	public static final int FunctionCallStyle = 13;
	public static final int ParameterStyle = 14;
	public static final int CommentStyle = 15;
	public static final int DocCommentStyle = 16;
	
	private final Object myLock = new Object();
	private boolean myShutdown;
	
	private final Map<String, String> myClassesMap = new HashMap<>(10000);
	private boolean myLoading;
	private boolean myHighlight;
	private boolean myHighlightSmali;
	private HighlightModel mySmaliModel;
	private final SmaliFlexLexer mySmaliLexer = new SmaliFlexLexer(28);
	private final ExtendSmaliParser myParser = new ExtendSmaliParser(null);
	private final Object myHighlightLock = new Object();
	private final HighlightSpace myHighlightSpace = new HighlightSpace(10000);
	private final HighlightSpace mySyntaxHighlightSpace = new HighlightSpace(10000);
	private ErrorListener myErrorListener;
	private HighlightingListener myHighlightingListener;
	
	public CodeService() {
		Thread thread = new Thread(null, () -> {
			try {
				while (!myShutdown) {
					synchronized (myHighlightLock) {
						if (myHighlight) {
							if (myHighlightSmali) {
								highlightSmali();
								myHighlightSmali = false;
							}
							myHighlight = false;
						}
						myHighlightLock.wait(50L);
					}
				
					
					synchronized (myLock) {
						if (myLoading) {
							doLoad();
							myLoading = false;
						}
						myLock.wait(200L);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, "CodeEngine", 8 * 1024 * 1024);
		thread.setPriority(2);
		thread.start();
	}
	
	public void setHighlightingListener(HighlightingListener highlightingListener) {
		myHighlightingListener = highlightingListener;
	}
	
	public void load() {
		synchronized (myLock) {
			myLoading = true;
			myLock.notify();
		}
	}
	
	private void doLoad() {
		
		File dest = new File(App.getContext().getFilesDir(), "android");
		if (dest.exists()) {
			loadClasses();
			return;
		}
		try {
			extractZip(App.getContext().getAssets().open("android.zip"), dest.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		loadClasses();
	}
	
	private void loadClasses() {
		myClassesMap.clear();
		File dest = new File(App.getContext().getFilesDir(), "android");
		myClassesMap.put("Lcom/myapp/app/Main;", "Main.smali");
		findFile(dest, dest, ".smali", myClassesMap);
	}
	
	
	public void setErrorListener(ErrorListener errorListener) {
		myErrorListener = errorListener;
	}
	
	private void highlightSmali() {
		var errors = new ArrayList<Error>();
		myParser.clearErrors();
		
		myHighlightSpace.reset();
		mySyntaxHighlightSpace.reset();
		Reader reader = new StringReader(mySmaliModel.getText());
		myParser.reset();
		myParser.setVerboseErrors(true);
		myParser.setApiLevel(28);
		mySmaliLexer.yyreset(reader);
		mySmaliLexer.yybegin(0);
		mySmaliLexer.setSuppressErrors(true);
		try {
			CommonToken token = (CommonToken) mySmaliLexer.yylex();
			int style = token.getType();
			int start=token.getStartIndex();
			int end=token.getStopIndex();
			if (token instanceof InvalidToken) {
				errors.add(new Error(
						token.getLine(),
						token.getCharPositionInLine() + 1,
						token.getLine(),
						token.getCharPositionInLine() + 1 + token.getText().length(), Error.ERROR));
			}
			while (true) {
				CommonToken nextToken = (CommonToken) mySmaliLexer.yylex();
				int nextStyle = nextToken.getType();
				int nextStart=nextToken.getStartIndex();
				int nextStop =nextToken.getStopIndex();
			
				int rStyle = getStyle(style);
				myHighlightSpace.highlight(rStyle, start,end);
				
				
				if (nextToken instanceof InvalidToken) {
					errors.add(new Error(
							nextToken.getLine(),
							nextToken.getCharPositionInLine() + 1,
							nextToken.getLine(),
							nextToken.getCharPositionInLine() + 1 + nextToken.getText().length(), Error.ERROR));
				}
				style = nextStyle;
				start = nextStart;
				end = nextStop;
				
				if (nextStyle == -1) break;
			/*	myHighlightSpace.highlight(0,
						startLine, startColumn,
						line, column);*/
			}
			
			myHighlightingListener.highlighting(myHighlightSpace.styles,
					myHighlightSpace.starts,
					myHighlightSpace.ends,
					myHighlightSpace.mySize);
			
			
		
				reader.reset();
				mySmaliLexer.yyreset(reader);
				mySmaliLexer.yybegin(0);
				
				mySmaliLexer.setSuppressErrors(true);
				var tokens = new CommonTokenStream(mySmaliLexer);
				myParser.setTokenStream(tokens);
				myParser.setBacktrackingLevel(1);
				
				
				smali_file_return smaliFile = myParser.smali_file();
				var tree = smaliFile.getTree();
				
				var visitor = new TreeVisitor() {
					@Override
					public Object visit(Object o, TreeVisitorAction treeVisitorAction) {
						CommonTree tree = (CommonTree) o;
						var list = new ArrayList<Error>();
						visitTree(tree, list);
						errors.addAll(list);
						return super.visit(o, treeVisitorAction);
					}
				};
				
				visitor.visit(tree, new TreeVisitorAction() {
					@Override
					public Object pre(Object o) {
						return o;
					}
					
					@Override
					public Object post(Object o) {
						return o;
					}
				});
				
				if (myParser.getNumberOfSyntaxErrors()==0) {
					myHighlightingListener.semanticHighlighting(mySyntaxHighlightSpace.styles,
							mySyntaxHighlightSpace.starts,
							mySyntaxHighlightSpace.ends,
							mySyntaxHighlightSpace.mySize);
				}
				
				errors.addAll(myParser.getErrors());
				
				var treeStream = new CommonTreeNodeStream(tree);
				treeStream.setTokenStream(tokens);
				var walker = new ExtendSmaliTreeWalker(treeStream);
				var dexGen = new DexBuilder(Opcodes.forApi(28));
				walker.setTreeNodeStream(treeStream);
				walker.setApiLevel(28);
				walker.setDexBuilder(dexGen);
				walker.setVerboseErrors(true);
				walker.smali_file();
				errors.addAll(walker.getErrors());
			
			myErrorListener.syntaxErrors(errors);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IoUtils.safeClose(reader);
		}
	}
	
	private void visitTree(CommonTree tree, List<Error> errors) {
		switch (tree.getType()) {
			case I_ANNOTATION_ELEMENT -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							mySyntaxHighlightSpace.highlight(TypeStyle,
									start,end);
						}
					}
				}
			}
			
			case I_LABEL -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							//AppLog.i("Label " + token.getText());
							mySyntaxHighlightSpace.highlight(MetadataStyle,
								start,end);
						}
					}
				}
			}
			
			case CLASS_DESCRIPTOR -> {
				CommonToken token = (CommonToken) tree.getToken();
				int start =token.getStartIndex();
				int end =token.getStopIndex();
				int startLine = token.getLine() - 1;
				int startColumn = token.getCharPositionInLine();
				int endLine = token.getLine() - 1;
				int endColumn = startColumn + token.getText().length();
				
				if (!myClassesMap.containsKey(token.getText())) {
					errors.add(new Error(startLine + 1,
							startColumn + 1,
							endLine + 1,
							endColumn, Error.ERROR));
				}
				//AppLog.i("Class " + token.getText());
				mySyntaxHighlightSpace.highlight(TypeStyle,
						start,end);
			}
			
			
			case I_STATEMENT_FORMAT21c_FIELD,
					I_STATEMENT_FORMAT22c_FIELD,
					I_FIELD -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							//AppLog.i("Field "+token.getText());
							mySyntaxHighlightSpace.highlight(VariableStyle,
									start,end);
						}
					}
				}
				
			}
			
			case I_METHOD -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							//AppLog.i("method "+token.getText());
							mySyntaxHighlightSpace.highlight(FunctionStyle,
									start,end);
						}
					}
				}
			}
			case I_CATCH, I_CATCHALL -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							//AppLog.i("Catch "+token.getText());
							mySyntaxHighlightSpace.highlight(MetadataStyle,
									start,end);
						}
					}
				}
			}
/*       case  I_STATEMENT_ARRAY_DATA,
           I_STATEMENT_FORMAT10t ,
           I_STATEMENT_FORMAT10x ,
           I_STATEMENT_FORMAT11n ,
           I_STATEMENT_FORMAT11x ,
           I_STATEMENT_FORMAT12x ,
           I_STATEMENT_FORMAT20bc,
           I_STATEMENT_FORMAT20t ,
           I_STATEMENT_FORMAT21c_METHOD_HANDLE ,
           I_STATEMENT_FORMAT21c_METHOD_TYPE ,
           I_STATEMENT_FORMAT21c_STRING,
           I_STATEMENT_FORMAT21c_TYPE,
           I_STATEMENT_FORMAT21ih,
           I_STATEMENT_FORMAT21lh,
           I_STATEMENT_FORMAT21s ,
           I_STATEMENT_FORMAT21t ,
           I_STATEMENT_FORMAT22b ,
           I_STATEMENT_FORMAT22c_TYPE ,
           I_STATEMENT_FORMAT22s ,
           I_STATEMENT_FORMAT22t ,
           I_STATEMENT_FORMAT22x ,
           I_STATEMENT_FORMAT23x ,
           I_STATEMENT_FORMAT30t ,
           I_STATEMENT_FORMAT31c ,
           I_STATEMENT_FORMAT31i ,
           I_STATEMENT_FORMAT31t ,
           I_STATEMENT_FORMAT32x ,
           I_STATEMENT_FORMAT35c_CALL_SITE ,
           I_STATEMENT_FORMAT35c_TYPE,
           I_STATEMENT_FORMAT3rc_CALL_SITE,
           I_STATEMENT_FORMAT3rc_TYPE,
           I_STATEMENT_FORMAT51l ,
           I_STATEMENT_PACKED_SWITCH ,
           I_STATEMENT_SPARSE_SWITCH->{*/
			
			case I_STATEMENT_FORMAT10t,
					I_STATEMENT_FORMAT21t,
					I_STATEMENT_FORMAT22t -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							//AppLog.i("Stament "+token.getText());
							mySyntaxHighlightSpace.highlight(MetadataStyle,
									start,end);
						}
					}
				}
			}
			
			case I_STATEMENT_FORMAT4rcc_METHOD,
					I_STATEMENT_FORMAT3rc_METHOD,
					I_STATEMENT_FORMAT35c_METHOD,
					I_STATEMENT_FORMAT45cc_METHOD -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							//AppLog.i("Method Call " + token.getText());
							mySyntaxHighlightSpace.highlight(FunctionCallStyle,
									start,end);
						}
					}
				}
			}
			
			default -> {
				if (tree.getChildCount() == 0) return;
				
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
                          /*  Token token = commonTree.getToken();
                            CommonTree parent = (CommonTree) commonTree.getParent();*/
							//   AppLog.i("Name " + token.getText() + " Parent " + parent.getText());
						}
						case MEMBER_NAME -> {
                      /*      Token token = commonTree.getToken();
                            CommonTree parent = (CommonTree) commonTree.getParent();*/
							// AppLog.i("Member Name " + token.getText() + " Parent " + parent.getText());
						}
						case CHAR_LITERAL, STRING_LITERAL -> {
							CommonToken token = (CommonToken) commonTree.getToken();
							int start =token.getStartIndex();
							int end =token.getStopIndex();
							mySyntaxHighlightSpace.highlight(StringStyle,
									start,end);
						}
					}
				}
			}
		}
	}
	
	private int getStyle(int type) {
		switch (type) {
			case CLASS_DIRECTIVE:
			case SUPER_DIRECTIVE:
			case IMPLEMENTS_DIRECTIVE:
			case SOURCE_DIRECTIVE:
			case FIELD_DIRECTIVE:
			case END_FIELD_DIRECTIVE:
			case SUBANNOTATION_DIRECTIVE:
			case END_SUBANNOTATION_DIRECTIVE:
			case ANNOTATION_DIRECTIVE:
			case END_ANNOTATION_DIRECTIVE:
			case ENUM_DIRECTIVE:
			case METHOD_DIRECTIVE:
			case END_METHOD_DIRECTIVE:
			case REGISTERS_DIRECTIVE:
			case LOCALS_DIRECTIVE:
			case ARRAY_DATA_DIRECTIVE:
			case END_ARRAY_DATA_DIRECTIVE:
			case PACKED_SWITCH_DIRECTIVE:
			case END_PACKED_SWITCH_DIRECTIVE:
			case SPARSE_SWITCH_DIRECTIVE:
			case END_SPARSE_SWITCH_DIRECTIVE:
			case CATCH_DIRECTIVE:
			case CATCHALL_DIRECTIVE:
			case LINE_DIRECTIVE:
			case PARAMETER_DIRECTIVE:
			case END_PARAMETER_DIRECTIVE:
			case LOCAL_DIRECTIVE:
			case END_LOCAL_DIRECTIVE:
			case RESTART_LOCAL_DIRECTIVE:
			case PROLOGUE_DIRECTIVE:
			case EPILOGUE_DIRECTIVE:
			
			case ANNOTATION_VISIBILITY:
			case ACCESS_SPEC:
			case HIDDENAPI_RESTRICTION:
			case VERIFICATION_ERROR_TYPE:
			case INLINE_INDEX:
			case VTABLE_INDEX:
			case FIELD_OFFSET:
			case METHOD_HANDLE_TYPE_FIELD:
			case METHOD_HANDLE_TYPE_METHOD:
			
			case INSTRUCTION_FORMAT10t:
			case INSTRUCTION_FORMAT10x:
			case INSTRUCTION_FORMAT10x_ODEX:
			case INSTRUCTION_FORMAT11n:
			case INSTRUCTION_FORMAT11x:
			case INSTRUCTION_FORMAT12x_OR_ID:
			case INSTRUCTION_FORMAT12x:
			case INSTRUCTION_FORMAT20bc:
			case INSTRUCTION_FORMAT20t:
			case INSTRUCTION_FORMAT21c_FIELD:
			case INSTRUCTION_FORMAT21c_FIELD_ODEX:
			case INSTRUCTION_FORMAT21c_STRING:
			case INSTRUCTION_FORMAT21c_TYPE:
			case INSTRUCTION_FORMAT21c_METHOD_HANDLE:
			case INSTRUCTION_FORMAT21c_METHOD_TYPE:
			case INSTRUCTION_FORMAT21ih:
			case INSTRUCTION_FORMAT21lh:
			case INSTRUCTION_FORMAT21s:
			case INSTRUCTION_FORMAT21t:
			case INSTRUCTION_FORMAT22b:
			case INSTRUCTION_FORMAT22c_FIELD:
			case INSTRUCTION_FORMAT22c_FIELD_ODEX:
			case INSTRUCTION_FORMAT22c_TYPE:
			case INSTRUCTION_FORMAT22cs_FIELD:
			case INSTRUCTION_FORMAT22s_OR_ID:
			case INSTRUCTION_FORMAT22s:
			case INSTRUCTION_FORMAT22t:
			case INSTRUCTION_FORMAT22x:
			case INSTRUCTION_FORMAT23x:
			case INSTRUCTION_FORMAT30t:
			case INSTRUCTION_FORMAT31c:
			case INSTRUCTION_FORMAT31i_OR_ID:
			case INSTRUCTION_FORMAT31i:
			case INSTRUCTION_FORMAT31t:
			case INSTRUCTION_FORMAT32x:
			case INSTRUCTION_FORMAT35c_CALL_SITE:
			case INSTRUCTION_FORMAT35c_METHOD:
			case INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE:
			case INSTRUCTION_FORMAT35c_METHOD_ODEX:
			case INSTRUCTION_FORMAT35c_TYPE:
			case INSTRUCTION_FORMAT35mi_METHOD:
			case INSTRUCTION_FORMAT35ms_METHOD:
			case INSTRUCTION_FORMAT3rc_CALL_SITE:
			case INSTRUCTION_FORMAT3rc_METHOD:
			case INSTRUCTION_FORMAT3rc_METHOD_ODEX:
			case INSTRUCTION_FORMAT3rc_TYPE:
			case INSTRUCTION_FORMAT3rmi_METHOD:
			case INSTRUCTION_FORMAT3rms_METHOD:
			case INSTRUCTION_FORMAT45cc_METHOD:
			case INSTRUCTION_FORMAT4rcc_METHOD:
			case INSTRUCTION_FORMAT51l:
				
				return KeywordStyle;
			case PARAM_LIST_OR_ID_PRIMITIVE_TYPE:
			case PRIMITIVE_TYPE:
			case VOID_TYPE:
			case CLASS_DESCRIPTOR:
			case ARRAY_TYPE_PREFIX:
				return TypeStyle;
			
			case DOTDOT:
			case ARROW:
			case EQUAL:
			case COLON:
			case COMMA:
			case OPEN_BRACE:
			case CLOSE_BRACE:
			case OPEN_PAREN:
			case CLOSE_PAREN:
				return SeparatorStyle;
			
			case POSITIVE_INTEGER_LITERAL:
			case NEGATIVE_INTEGER_LITERAL:
			case LONG_LITERAL:
			case SHORT_LITERAL:
			case BYTE_LITERAL:
			case FLOAT_LITERAL_OR_ID:
			case DOUBLE_LITERAL_OR_ID:
			case FLOAT_LITERAL:
			case DOUBLE_LITERAL:
				return NumberStyle;
			case BOOL_LITERAL:
			case NULL_LITERAL:
				return StringStyle;
			case LINE_COMMENT:
				return CommentStyle;
			case WHITE_SPACE:
			default:
				return PlainStyle;
		}
	}
	
	
	public void highlightSmali(HighlightModel model) {
		synchronized (myHighlightLock) {
			myHighlight = true;
			myHighlightSmali = true;
			mySmaliModel = model;
			myHighlightLock.notify();
		}
	}
	
	public void shutdown() {
		synchronized (myLock) {
			myShutdown = true;
			myLock.notify();
		}
	}
	
	private void findFile(File root, File dir, String ext, Map<String, String> files) {
		if (dir.isDirectory()) {
			var list = dir.listFiles();
			if (list != null) {
				for (File file : list) {
					findFile(root, file, ext, files);
				}
			}
		} else if (dir.isFile() && dir.getName().endsWith(ext)) {
			String path = dir.getAbsolutePath();
			String path2 = path.substring(root.getAbsolutePath().length() + 1);
			String name = path2.substring(0, path2.length() - 6);
			String key = "L" + name + ";";
			AppLog.i(key);
			files.put(key, path);
		}
	}
	
	
	public interface ErrorListener {
		void syntaxErrors(List<Error> errors);
	}
	
	public interface HighlightingListener {
		void highlighting(int[] styles,int[] starts,int[] ends,int size);
		void semanticHighlighting(int[] styles,int[] starts,int[] ends,int size);
	
	}
	
	public static class Error {
		public static final int ERROR = 0;
		public static final int WARNING = 1;
		public static final int DEPRECATED = 2;
		public int startLine;
		public int startColumn;
		public int endLine;
		public int endColumn;
		public int level;
		
		public Error(int startLine, int startColumn, int endLine, int endColumn, int level) {
			this.startLine = startLine;
			this.startColumn = startColumn;
			this.endLine = endLine;
			this.endColumn = endColumn;
			this.level = level;
		}
		
		@NonNull
		@Override
		public String toString() {
			return "Error(" + startLine + ", " + startColumn + ", " + endLine + ", " + endColumn + ")";
		}
	}
	
	public static void extractZip(@NonNull InputStream zipInput, @NonNull String targetPath) throws IOException {
		var zipStream = new ZipInputStream(zipInput);
		ZipEntry entry;
		while ((entry = zipStream.getNextEntry()) != null) {
			var entryPath = entry.getName();
			var entryTargetPath = targetPath + separator + entryPath;
			if (entry.isDirectory()) {
				new File(entryTargetPath).mkdirs();
			} else if (!new File(entryTargetPath).isFile()) {
				var parentPath = getParent(entryTargetPath);
				if (parentPath != null) new File(parentPath).mkdirs();
				var fileOut = new FileOutputStream(entryTargetPath);
				IoUtils.copy(zipStream, fileOut);
				fileOut.close();
			}
		}
	}
	
	public static String getParent(@NonNull String filePath) {
		if (filePath.isEmpty())
			return null;
		if (filePath.equals(separator))
			return null;
		int index = filePath.lastIndexOf(separatorChar);
		if (index == 0) {
			return separator;
		} else if (index == -1) {
			return null;
		}
		return filePath.substring(0, index);
	}
}
