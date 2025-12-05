package primula.api.core.test;

public class ArgClass {
    private String type;
    private char ctype;
    private int itype;
    private double dtype;
    private Data data;

    public ArgClass(String str, char c, int i) {
        type = str;
        ctype = c;
        itype = i;
        data = new Data();
    }

    public ArgClass(String str, char c, double d) {
        type = str;
        ctype = c;
        dtype = d;
        data = new Data();
    }


    public void printField() {
        System.out.println(type);
        System.out.println(ctype);
        System.out.println(itype);
    }
}
