package weg.ide.tools.smali.ui;


import java.util.Arrays;

public class Spans {
	private byte[] myStyles = new byte[100];
	
	private int getSize() {
		if (myStyles.length == 0)
			return 0;
		
		byte lastStyle = myStyles[myStyles.length - 1];
		for (int length = myStyles.length - 2; length >= 0; length--) {
			if (myStyles[length] != lastStyle) {
				return length + 2;
			}
		}
		return 0;
	}
	
	private void span(byte style, int start, int end) {
		resize(end + 1);
		for (int i = start; i <= end; i++) {
			myStyles[i] = style;
		}
	}
	
	
	public void insert(int startColumn, int endColumn) {
		resize(myStyles.length + (endColumn-startColumn));
		
		int endIndex = startColumn + endColumn;
		System.arraycopy(myStyles, startColumn, myStyles, endIndex, (myStyles.length - startColumn) - endColumn);
		byte lastStyle = startColumn > 0 ? myStyles[startColumn - 1] : (byte) 0;
		
		for (int i = startColumn; i < endIndex; i++) {
			myStyles[i] = lastStyle;
		}
		
	}
	
	
	public void remove(int start, int end) {
		if (end + 1 < myStyles.length) {
			System.arraycopy(myStyles, end + 1, myStyles, start + 1, (myStyles.length - end) - 1);
		}
	}
	
	private void resize(int count) {
		byte[] styles = myStyles;
		if (styles.length <= count) {
			byte lastStyle = styles[styles.length - 1];
			int size = ((count * 5) / 4) + 1;
			byte[] newStyles = new byte[size];
			System.arraycopy(styles, 0, newStyles, 0, styles.length);
			myStyles = newStyles;
			for (int length = styles.length; length < size; length++) {
				newStyles[length] = lastStyle;
			}
		}
	}
	
	public byte getStyle(int offset) {
		if (offset >= myStyles.length)
			return 0;
		
		return myStyles[offset];
	}
	
	public void clear() {
		Arrays.fill(myStyles, (byte) 0);
	}
	
	public void set(int[] styles, int[] starts, int[] ends, int size) {
		clear();
		for (int i = size - 1; i >= 0; i--) {
			if (styles[i] != -1)
				span((byte) styles[i], starts[i], ends[i]);
		}
	}
	
}
