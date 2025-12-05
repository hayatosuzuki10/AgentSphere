package primula.api.core.assh.test;

public class Ex1 {
    private String str;
    private int x;
    //private Data data;

    public Ex1() {
        str = "Hello World";
        x = 10;
        //data = new Data();
    }

    public void print() {
        System.out.println(str);
    }

    public void cal(int n0, int n1) {
        int sum = x + n0 + n1;
        System.out.println(sum);
    }

    public String getString() {
        return str;
    }
}
