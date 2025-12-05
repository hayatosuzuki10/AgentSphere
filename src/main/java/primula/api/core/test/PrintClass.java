package primula.api.core.test;

public class PrintClass {
    private String type;
    private char ctype;
    private byte btype;
    private short stype;
    private int itype;
    private long ltype;
    private float ftype;
    private double dtype;
    private boolean booleantype;
    private Data data;
    private Data data1;
    private Data data2;
    private Data data3;

    public PrintClass() {
        type = "type";
        ctype = 'C';
        btype = 64;
        stype = 128;
        itype = 256;
        ltype = 512;
        ftype = 1024;
        dtype = 2048;
        booleantype = true;
        data = new Data();
        data1 = new Data();
        data2 = new Data();
        data3 = new Data();
    }

    public void printField() {
        System.out.println("type = " + type);
        System.out.println("ctype = " + ctype);
        System.out.println("btype = " + btype);
        System.out.println("stype = " + stype);
        System.out.println("itype = " + itype);
        System.out.println("ltype = " + ltype);
        System.out.println("ftype = " + ftype);
        System.out.println("dtype = " + dtype);
        System.out.println("booleantype = " + booleantype);
        //System.out.println("toString = " + data.getClass().getName());
    }

    public void printCalTest(String str, int x, int y) {
        int sum = x + y;
        System.out.println(str + " : " + sum);
    }

    public void printCalTest(char str, int x, int y) {
        int sum = x + y;
        System.out.println(str + " : " + sum);
    }
}
