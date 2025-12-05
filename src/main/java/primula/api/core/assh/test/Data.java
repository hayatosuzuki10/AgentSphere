package primula.api.core.assh.test;

public class Data {
    public String type;
    public char ctype;
    public byte btype;
    public short stype;
    public int itype;
    public long ltype;
    public float ftype;
    public double dtype;
    public boolean booleantype;
    public Ex1 ex;

    public Data() {
        type = "changeOK!";
        ctype = 'A';
        btype = 100;
        stype = 100;
        itype = 100;
        ltype = 100;
        ftype = 100;
        dtype = 100;
        booleantype = false;
        ex = new Ex1();
    }

    public Data(String str) {
        type = str;
        ctype = 'A';
        btype = 100;
        stype = 100;
        itype = 100;
        ltype = 100;
        ftype = 100;
        dtype = 100;
        booleantype = false;
        ex = new Ex1();
    }

    public Data(int x, String str) {
        type = str;
        ctype = 'A';
        btype = 100;
        stype = 100;
        itype = x;
        ltype = 100;
        ftype = 100;
        dtype = 100;
        booleantype = false;
        ex = new Ex1();
    }

    public Data(int x) {
        type = "aaa";
        ctype = 'A';
        btype = 100;
        stype = 100;
        itype = x;
        ltype = 100;
        ftype = 100;
        dtype = 100;
        booleantype = false;
        ex = new Ex1();
    }

    public String getString() {
        return type;
    }

    public String getS(String s) {
    	return s;
    }

    public String getArgString(String newType) {
        return newType;
    }

    public String getArgString(String newType, int test) {
        return newType;
    }

    public char getChar() {
        return ctype;
    }

    public byte getByte() {
        return btype;
    }

    public short getShort() {
        return stype;
    }

    public int getInt() {
        return itype;
    }

    public int getArgInt(int newItype) {
        return newItype;
    }

    public long getLong() {
        return ltype;
    }

    public float getFloat() {
        return ftype;
    }

    public double getDouble() {
        return dtype;
    }

    public boolean getBoolean() {
        return booleantype;
    }

    public boolean getBoolean(String str) {
        if(str.equals("abc")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean getBoolean(String str, int i, char ch) {
        return true;
    }

    public String getStringPlus() {
        return ex.getString();
    }

    public void print(String str, int x, int y) {
    	System.out.println(str);
    	System.out.println(x);
    }

    public void p(int a, int x, int y) {
    	System.out.println(a);
    	System.out.println(x);
    }

    public static void stPrint(String str) {
    	System.out.println(str);
    }
}
