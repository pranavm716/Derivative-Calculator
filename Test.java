import java.util.ArrayList;

public class Test {
    public static void main(String[] args) {
        // String infix = "2 + x + 4 + 4";
        String infix = "ln(sin(x))";
        // String infix = "x+x^9+sin(x)^2/8";
        // String infix = "2 * a * 3 * x - 90 * x + 4 + 8 / 4 + 5";
        // String infix = "(x+1/x)^(-4)";

        
        // Breaking cases


        Expression c = new Expression(infix, "x");

        // System.out.println(c.getLeaves(c.derivativeTree, new ArrayList<Node>()));
    }
}