package weg.ide.tools.smali.common;

public class HighlightSpace {

    public int[] styles;
    public int[] starts;
    public int[] ends;
    public int mySize = 0;

    public HighlightSpace(int initSize) {
        styles = new int[initSize];
        starts = new int[initSize];
        ends = new int[initSize];
        mySize = 0;
    }

    public void reset() {
        mySize = 0;
    }

    public void highlight(int type, int start,int end) {
        resize(mySize);
        styles[mySize] = type;
        starts[mySize] = start;
        ends[mySize] = end;
       
        mySize++;
    }

    private void resize(int size) {
        if (styles.length <= size) {
            int[] types = new int[size * 5 / 4];
            System.arraycopy(styles, 0, types, 0, styles.length);
            styles = types;

            int[] ints = new int[size * 5 / 4];
            System.arraycopy(starts, 0, ints, 0, starts.length);
            starts = ints;

            int[] ints3 = new int[size * 5 / 4];
            System.arraycopy(ends, 0, ints3, 0, ends.length);
            ends = ints3;

        }
    }
}
