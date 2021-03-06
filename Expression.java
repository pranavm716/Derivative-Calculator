import java.util.*;

public class Expression {
    // OPERATORS
    final String ADD = "+";
    final String SUB = "-";
    final String MUL = "*";
    final String DIV = "/";
    final String POW = "^";
    final String[] OPERATORS = {ADD, SUB, MUL, DIV, POW};

    // FUNCTIONS
    final String SIN = "sin";
    final String CSC = "csc";
    final String COS = "cos";
    final String SEC = "sec";
    final String TAN = "tan";
    final String COT = "cot";
    final String LN  = "ln";
    final String SQRT = "sqrt";
    final String ABS = "abs";
    final String[] FUNCTIONS = {SIN, CSC, COS, SEC, TAN, COT, LN, SQRT, ABS};

    // CONSTANTS
    final String E = "e";
    final String PI = "pi";
    final String[] CONSTANTS = {E, PI};

    // OTHER
    final String OPEN_PAREN = "(";
    final String CLOSE_PAREN = ")";

    String infix;
    String[] infixArray;
    String[] postfixArray;
    Node originalExpressionTree;
    Node simplifiedExpressionTree;
    Node derivativeTree;
    String wrt;

    // TODO: Distribute negative signs?
    // TODO: Handles pi as an input to trig functions (evaluate)
    // TODO: Factor only constant nodes when simplifying OG expression, can factor everything when simplifying derivative
    // TODO: Find out how to keep same order of subtrees after commutative swapping
    public Expression(String infix, String wrt) {
        this.infix = infix;
        this.wrt = wrt;

        preProcessInfix();
        System.out.println(this.infix);
        this.infixArray = this.infix.split(" ");

        convertToPostfix();
        generateExpressionTree();
        simplifiedExpressionTree = simplify(originalExpressionTree);

        simplifiedExpressionTree.print();

        System.out.printf("Original: f(%s) = ", wrt);
        printTreeInfix(originalExpressionTree);
        System.out.println();

        System.out.printf("Simplified: f(%s) = ", wrt);
        printTreeInfix(simplifiedExpressionTree);
        System.out.println();

        differentiate();
        derivativeTree.print();
        System.out.printf("df(%s)/d%s = ", wrt, wrt);
        printTreeInfix(derivativeTree);
        System.out.println();
    }

    // TODO: Handle solitary (-) signs
    // TODO: Handle nested parentheses 
    public void preProcessInfix(){
        infix = infix.trim();
        infix = infix.replaceAll("\\s","");
        String processed = "";

        for(int i = 0; i < infix.length(); i++){
            String token = infix.substring(i, i + 1);
            for(String function : FUNCTIONS){
                if(infix.indexOf(function, i) == i) token = function;
            }
            for(String constant : CONSTANTS){
                if(infix.indexOf(constant, i) == i) token = constant;
            }

            if(i > 0 && !("" + infix.charAt(i - 1)).equals(OPEN_PAREN) && (isLetteredConstant(token) || isVariable(token) || isFunction(token) || isPredefinedConstant(token))){
                if(isFunction(token)){
                    if(!isOperator("" + infix.charAt(i - 1))) processed += MUL;
                    processed += token + OPEN_PAREN;
                    if(!("" + infix.charAt(i + token.length())).equals(OPEN_PAREN)){
                        int nextOperatorIndex = findNextOperatorOrFunctionIndex(infix, i);
                        infix = infix.substring(0, i + token.length()) + OPEN_PAREN + infix.substring(i + token.length(), nextOperatorIndex) + CLOSE_PAREN + infix.substring(nextOperatorIndex);
                    }
                    i++;
                }else if (isFunction(token) || !isOperator("" + infix.charAt(i - 1))){
                    processed += MUL + token;
                }else{
                    processed += token;
                }
            }
            else if (i > 0 && token.equals(OPEN_PAREN)){
                if(!isOperator("" + infix.charAt(i - 1)) && !("" + infix.charAt(i - 1)).equals(OPEN_PAREN)) processed += MUL;
                processed += token;
            }
            else{
                if(isFunction(token)){
                    processed += token + OPEN_PAREN;
                    if(!("" + infix.charAt(i + token.length())).equals(OPEN_PAREN)){
                        int nextOperatorIndex = findNextOperatorOrFunctionIndex(infix, i);
                        infix = infix.substring(0, i + token.length()) + OPEN_PAREN + infix.substring(i + token.length(), nextOperatorIndex) + CLOSE_PAREN + infix.substring(nextOperatorIndex);
                    }
                    i++;
                }else if (token.equals(SUB)){
                    processed += ","; // Use comma as unary negate sign, will get replaced later
                    if ((i > 0 && (infix.charAt(i - 1) == '(')) || i == 0) processed += "1*";
                }else{
                    processed += token;
                }
            }

            i += token.length() - 1;
        }
        
        infix = processed;
        
        for(String constant : CONSTANTS){ // Add spaces around constants
            infix = infix.replace(constant, " " + constant + " ");
        }
        
        for(String function : FUNCTIONS){ // Add spaces around functions
            infix = infix.replace(function, " " + function + " ");
        }

        
        for(String operator : OPERATORS){ // Add spaces around operators
            infix = infix.replace(operator, " " + operator + " ");
        }

        infix = infix.replace(OPEN_PAREN, " ( "); // Add spaces around parentheses
        infix = infix.replace(CLOSE_PAREN, " ) ");
        infix = infix.replaceAll(",", " - "); // Replace all streaks of multiple spaces with just 1 space
        infix = infix.trim().replaceAll(" +", " "); // Replace all streaks of multiple spaces with just 1 space
    }

    private int findNextOperatorOrFunctionIndex(String str, int index){
        int pos = str.length();
        for(String operator : OPERATORS){
            int curOperatorIndex = str.indexOf(operator, index);
            if(curOperatorIndex != -1) pos = Math.min(pos, curOperatorIndex);
        }

        for(String function : FUNCTIONS){
            int curFunctionIndex = str.indexOf(function, index);
            if(curFunctionIndex != -1 && curFunctionIndex > index) pos = Math.min(pos, curFunctionIndex);
        }

        return pos;
    }

    public void differentiate() {
        Node derivative = simplifiedExpressionTree.copy();
        derivativeTree = simplify(recDifferentiate(derivative));
    }

    public Node recDifferentiate(Node derivative) {
        String token = derivative.token;
        Node copy = derivative.copy();
        Node l = copy.left, r = copy.right;

        if (isOperator(token)) {
            /*
             * Break up into 4 cases to simplify the tree: 
             * Case 1: l and r are both constants (a & b respectively) 
             * Case 2: l is constant (a) and r is a function 
             * Case 3: l is a function and r is a constant (b) 
             * Case 4: l and r are both functions
             */

            switch (token) {
                case MUL: // Apply appropriate product rule
                
                    if (evaluatesToConstant(l) && evaluatesToConstant(r)) { // Case 1: Constant (constant multipled by a constant)
                        // D(a*b) = 0
                        derivative = new Node(0);
                    } else if (evaluatesToConstant(l)) { // Case 2: Constant multiple (constant multiplied by a funciton)
                        // D(a*r) = a * D(r)
                        derivative.left = evaluateConstantNode(l);
                        derivative.right = recDifferentiate(r.copy());
                    }else if(evaluatesToConstant(r)){ // Case 3: Constant multiple (function multiplied by a constant)
                        // D(l*b) = D(l) * b
                        derivative.left = recDifferentiate(l.copy());
                        derivative.right = evaluateConstantNode(r);
                    }else{ // Case 4: Product of functions (function multiplied by a function)
                        // Modify signs
                        derivative.token = ADD;
                        derivative.left.token = MUL;
                        derivative.right.token = MUL;

                        // D(l*r) = l * D(r) + r * D(l)
                        derivative.left.left = l;
                        derivative.left.right = recDifferentiate(r.copy());
                        derivative.right.left = r;
                        derivative.right.right = recDifferentiate(l.copy());
                    }
                    
                    break;

                case DIV: // Apply appropriate quotient rule
                    if (evaluatesToConstant(l) && evaluatesToConstant(r)) { // Case 1: Constant (constant divided by a constant)
                        // D(a/b) = 0
                        derivative = new Node(0);
                    } else if (evaluatesToConstant(l)) { // Case 2: Constant divided by a function
                        // Modify signs
                        derivative.token = MUL;
                        derivative.left.token = MUL;
                        if(derivative.left.has2Children() && derivative.left.right.has2Children()){ // Derivative is only guaranteed to have one level of children
                            derivative.left.right.token = DIV;
                            derivative.left.right.right.token = POW;
                        }else if (derivative.left.has2Children()){
                            derivative.left.right.token = DIV;
                            derivative.left.right.right = new Node(POW);
                        }else{
                            derivative.left.right = new Node(DIV);
                            derivative.left.right.right = new Node(POW);
                        }
                        
                        // D(a/r) = -1 * a / r^2 * D(r)
                        derivative.right = recDifferentiate(r.copy());
                        derivative.left.left = new Node(-1);
                        derivative.left.right.left = l;
                        derivative.left.right.right.left = r;
                        derivative.left.right.right.right = new Node(2);

                    }else if((evaluatesToConstant(r))){ // Case 3: Constant multiple (function divided by a constant)
                        // D(l/b) = D(l) / b
                        derivative.left = recDifferentiate(l.copy());
                        derivative.right = evaluateConstantNode(r);
                    }else{ // Case 4: Quotient of functions (function divided by a function)
                        // Modify signs
                        derivative.left.token = SUB;
                        derivative.right.token = POW;
                        if(derivative.left.has2Children()){
                            derivative.left.left.token = MUL;
                            derivative.left.right.token = MUL;
                        }else{
                            derivative.left.left = new Node(MUL);
                            derivative.left.right = new Node(MUL);
                        }

                        // D(l/r) = (r * D(l) - l * D(r)) / r^2
                        derivative.right.left = r;
                        derivative.right.right = new Node(2);
                        derivative.left.left.left = recDifferentiate(l.copy());
                        derivative.left.left.right = r;
                        derivative.left.right.left = l;
                        derivative.left.right.right = recDifferentiate(r.copy());
                    }
                    
                    break;
                case ADD: // Apply appropriate linearity rule
                case SUB:
                    if (evaluatesToConstant(l) && evaluatesToConstant(r)) { // Case 1: Constant (constant plus or minus constant)
                        // D(a??b) = 0
                        derivative = new Node(0);
                    } else if (evaluatesToConstant(l)) { // Case 2: Constant sum/ difference (constant plus or minus a function)
                        if(token.equals(ADD)){
                            // D(a+r) = D(r)
                            derivative = recDifferentiate(r.copy());
                        }else{ // Is a minus (-)
                            // Modify sign
                            derivative.token = MUL;

                            // D(a-r) = -1 * D(r)
                            derivative.left = new Node(-1);
                            derivative.right = recDifferentiate(r.copy());
                        }
                    }else if((evaluatesToConstant(r))){ // Case 3: Constant sum/ difference (function plus or minus a constant)
                        // D(l??b) = D(l)
                        derivative.left = recDifferentiate(l.copy());
                        derivative.right = new Node(0);
                    }else{ // Case 4: Linearity of functions
                        // D(l??r) = D(l) ?? D(r)
                        derivative.left = recDifferentiate(l.copy());
                        derivative.right = recDifferentiate(r.copy());
                    }

                    break;
                case POW: // Apply appropriate power rule
                    if (evaluatesToConstant(l) && evaluatesToConstant(r)) { // Case 1: Constant (Constant raised to a constant)
                        // D(a^b) = 0
                        derivative = new Node(0);
                    } else if (evaluatesToConstant(l)) { // Case 2: Exponential (Constant raised to a function)
                        // Modify signs
                        derivative.token = MUL;
                        derivative.left.token = MUL;
                        if(derivative.left.has2Children()){ // Derivative is only guaranteed to have one level of children
                            derivative.left.left.token = LN;
                            derivative.left.right.token = POW;
                        }else{
                            derivative.left.left = new Node(LN);
                            derivative.left.right = new Node(POW);
                        }
                        
                        // D(a^r) = naturalLog(a) * a^r * D(r)
                        derivative.right = recDifferentiate(r.copy());
                        derivative.left.left.left = l;
                        derivative.left.right.left = l;
                        derivative.left.right.right = r;
                    } else if (evaluatesToConstant(r)) { // Case 3: Polynomial (Function raised to a constant)
                        // Modify signs
                        derivative.token = MUL;
                        derivative.right.token = MUL;
                        
                        if(derivative.right.has2Children() && derivative.right.left.has2Children()){ // Derivative is only guaranteed to have one level of children
                            derivative.right.left.token = POW;
                            derivative.right.left.right.token = SUB;
                        }else if(derivative.right.has2Children()){
                            derivative.right.left.token = POW;
                            derivative.right.left.right = new Node(SUB);
                        }else{
                            derivative.right.left = new Node(POW);
                            derivative.right.left.right = new Node(SUB);
                        }

                        // D(l^b) = b * l^(b-1) * D(l)
                        derivative.left = recDifferentiate(l.copy());
                        derivative.right.right = r;
                        derivative.right.left.left = l;
                        derivative.right.left.right.left = r;
                        derivative.right.left.right.right = new Node(1);
                    }else{ // Case 4: Function raised to a function
                        // D(l^r) = (l^r) * (r/l * D(l) + naturalLog(l) * D(r))
                        Node Dl = recDifferentiate(l.copy());
                        Node Dr = recDifferentiate(r.copy());
                        Node lTor = derivative.copy();
                        Node rOverl = new Node(DIV, r.copy(), l.copy());
                        Node naturalLogl = new Node(LN, l.copy(), null);

                        derivative = new Node(MUL, lTor, new Node(ADD, new Node(MUL, rOverl, Dl), new Node(MUL, naturalLogl, Dr)));
                    }

                    break;
            }

        } else if (isFunction(token)) {
            /* Break up into 2 cases to simplify the tree: 
             * Case 1: Argument (l) is a constant (c)
             * Case 2: Argument (l) is a function
             */ 
            if(evaluatesToConstant(l)){ // Case 1: Argument is a constant
                // D(l(c)) = 0
                derivative = new Node(0);
            }else{ // Case 2: Argument is a function
                // All of these have a [* D(l)] in common
                derivative.token = MUL;
                derivative.left = recDifferentiate(l.copy());
                switch (token) {
                    case SIN:
                        // D(sin(l)) = cos(l) * D(l)
                        derivative.right = new Node(COS);
                        derivative.right.left = l;
                        break;
                    case CSC:
                        // D(csc(l)) = -1 * csc(l) * cot(l) * D(l)
                        derivative.right = new Node(MUL);
                        derivative.right.left = new Node(MUL);
                        derivative.right.left.left = new Node(-1);
                        derivative.right.left.right = new Node(CSC);
                        derivative.right.left.right.left = l;
                        derivative.right.right = new Node(COT);
                        derivative.right.right.left = l;
                        break;
                    case COS:
                        // D(cos(l)) = -1 * sin(l) * D(l)
                        derivative.right = new Node(MUL, new Node(-1), new Node(SIN, l, null));
                        break;
                    case SEC:
                        // D(sec(l)) = tan(l) * sec(l) * D(l)
                        derivative.right = new Node(MUL);
                        derivative.right.left = new Node(TAN);
                        derivative.right.left.left = l;
                        derivative.right.right = new Node(SEC);
                        derivative.right.right.left = l;
                        break;
                    case TAN:
                        // D(tan(l)) = (sec(l))^2 * D(l)
                        derivative.right = new Node(POW);
                        derivative.right.left = new Node(SEC);
                        derivative.right.left.left = l;
                        derivative.right.right = new Node(2);
                        break;
                    case COT:
                        // D(cot(l)) = -1 * (csc(l))^2 * D(l)
                        derivative.right = new Node(MUL);
                        derivative.right.left = new Node(-1);
                        derivative.right.right = new Node(POW);
                        derivative.right.right.left = new Node(CSC);
                        derivative.right.right.left.left = l;
                        derivative.right.right.right = new Node(2);
                        break;
                    case LN:
                        // D(naturalLog(l)) = 1/l * D(l)
                        derivative.right = new Node(DIV);
                        derivative.right.left = new Node(1);
                        derivative.right.right = l;
                        break;
                    case SQRT:
                        // D(sqrt(l)) = 0.5 / sqrt(l) * D(l)
                        derivative.right = new Node(DIV);
                        derivative.right.left = new Node(0.5);
                        derivative.right.right = new Node(SQRT);
                        derivative.right.right.left = l;
                        break;
                    case ABS:
                        // D(abs(l)) = l/abs(l) * D(l)
                        derivative.right = new Node(DIV);
                        derivative.right.left = l;
                        derivative.right.right = new Node(ABS);
                        derivative.right.right.left = l;
                        break;
                }
            }

        } else if (isVariable(token)) { // Is a variable
            derivative = new Node(1); // D(x) = 1
        } else { // Is a number
            derivative = new Node(0); // D(c) = 0
        }

        return derivative;
    }

    // public Node simplify(Node tree){
    //     Node original = tree.copy();
    //     Node simplified = tree.copy();
    //     int iterations = 0;
    //     int iterationBound = 50; // Avoids infinite loops due to commutative subtree swapping 

    //     do{
    //         original = simplified.copy();
    //         simplified = recSimplify(simplified, false);
    //         iterations++;
    //     } while (!original.equals(simplified) && iterations <= iterationBound);

    //     // simplified = consolidateNumbers(simplified);

    //     return simplified;
    // }

    public Node simplify(Node tree){
        return recSimplify(tree, false, false, tree.copy());
    }

    public Node recSimplify(Node tree, boolean onSubtreeSwap, boolean onSimplify, Node previous){
        tree = evaluateAllConstantNodes(tree);
        if(!onSubtreeSwap){
            tree = consolidateNumbers(tree);
            tree = evaluateAllConstantNodes(tree);
        }
        
        if(tree.has2Children()){
            switch(tree.token){
                case MUL:
                    if(numericEquals(tree.left, 1)){
                        // (1) * (s) = s
                        tree = tree.right.copy();
                    }

                    else if(numericEquals(tree.left, 0)){ 
                        // (0) * (s) = s
                        tree = new Node(0);
                    }
                    
                    else if (tree.right.equals(tree.left)){ 
                        // (s) * (s) = s ^ 2
                        tree.token = POW;
                        tree.right = new Node(2);
                    }
                    
                    else if(tree.left.token.equals(POW) && tree.right.token.equals(POW) && tree.left.left.equals(tree.right.left)){
                        // (s ^ a) * (s ^ b) = s ^ (a + b)
                        Node s = tree.left.left.copy();
                        Node a = tree.left.right.copy();
                        tree.token = POW;
                        tree.left = s;
                        tree.right.token = ADD;
                        tree.right.left = a;
                    }
                    
                    else if(tree.right.token.equals(POW) && tree.left.equals(tree.right.left)){
                        // (s) * (s ^ a) = s ^ (1 + a)
                        tree.token = POW;
                        tree.right.token = ADD;
                        tree.right.left = new Node(1);
                    }

                    else if(tree.right.token.equals(DIV) && isConstant(tree.right.left)){
                        // (s) * (a / t) = a * (s / t), where "a" is a constant
                        Node s = tree.left.copy();
                        Node a = tree.right.left.copy();
                        tree.left = a;
                        tree.right.left = s;
                    }

                    else{ // Multiplication is commutative, try swapping the subtrees
                        if(!onSubtreeSwap){
                            Node original = tree.copy();
                            tree.swapSubtrees();
                            Node newer = recSimplify(tree, true, false, original);

                            tree = newer;
                            if(original.equals(newer)) // Tree did not change, so can swap back
                                tree.swapSubtrees();
                        }
                    }

                    break;

                case SUB:
                    if(numericEquals(tree.right, 0)){ // (s) - (0) = s
                        tree = tree.left.copy();
                    }

                    else if(numericEquals(tree.left, 0)){ // (0) - (s) = -1 * s
                        tree = new Node(MUL, new Node(-1), tree.right.copy()); 
                    }
                    
                    else if(tree.left.equals(tree.right)){ // (s) - (s) = 0
                        tree = new Node(0); 
                    }
                    
                    else if (tree.right.token.equals(MUL) && isNegative(tree.right.left)){
                        // s - (-a * t) = s + (a * t)
                        tree.token = ADD;
                        negate(tree.right.left);
                    }else if (tree.right.token.equals(MUL) && isNegative(tree.right.right)){
                        // s - (t * -a) = s + (t * a)
                        tree.token = ADD;
                        negate(tree.right.right);
                    }
                    
                    else if (isNegative(tree.right)){
                        // s - (-a) = s + a
                        tree.token = ADD;
                        negate(tree.right);
                    }
                    
                    // TODO: "DUALS" of the below
                    else if (tree.right.token.equals(MUL) && tree.left.equals(tree.right.left)){
                        // s - (s * a) = s * (1 - a)
                        tree.token = MUL;
                        tree.right.token = SUB;
                        tree.right.left = new Node(1);
                    }else if (tree.right.token.equals(MUL) && tree.left.equals(tree.right.right)){
                        // s - (a * s) = s * (1 - a)
                        tree.token = MUL;
                        tree.right.token = SUB;
                        tree.right.right = new Node(1);
                        tree.right.swapSubtrees();
                    }

                    else if (tree.right.token.equals(DIV) && tree.left.equals(tree.right.left)){
                        // s - (s / a) = s * (1 - 1/a)
                        Node a = tree.right.right;
                        tree.token = MUL;
                        tree.right.token = SUB;
                        tree.right.left = new Node(1);
                        tree.right.right= new Node(DIV, new Node(1), a);
                    }
                    //

                    else if(tree.left.token.equals(MUL) && tree.right.token.equals(MUL)){
                        if(tree.left.right.equals(tree.right.right)){
                            // (a * s) - (b * s) = s * (a - b)
                            Node s = tree.left.right.copy();
                            Node a = tree.left.left.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = SUB;
                            tree.right.right = a;
                            tree.right.swapSubtrees();
                        }else if(tree.left.left.equals(tree.right.right)){
                            // (s * a) - (b * s) = s * (a - b)
                            Node s = tree.left.left.copy();
                            Node a = tree.left.right.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = SUB;
                            tree.right.right = a;
                            tree.right.swapSubtrees();
                        }else if(tree.left.right.equals(tree.right.left)){
                            // (a * s) - (s * b) = s * (a - b)
                            Node s = tree.left.right.copy();
                            Node a = tree.left.left.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = SUB;
                            tree.right.left = a;
                        }else if(tree.left.left.equals(tree.right.left)){
                            // (s * a) - (s * b) = s * (a - b)
                            Node s = tree.left.left.copy();
                            Node a = tree.left.right.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = SUB;
                            tree.right.left = a;
                        }
                    }

                    else if(tree.left.token.equals(DIV) && tree.right.token.equals(DIV) && tree.left.right.equals(tree.right.right)){
                        // (a / s) - (b / s) = (a - b) / s
                        Node a = tree.left.left.copy();
                        Node b = tree.right.left.copy();
                        Node s = tree.left.right.copy();
                        tree = new Node(DIV, new Node(SUB, a, b), s);
                    }


                    // TODO: // 1 - (sin(s) ^ 2) = (cos(s) ^ 2)
                    // TODO: // 1 - (cos(s) ^ 2) = (sin(s) ^ 2)
                    // TODO: // (sec(s) ^ 2) - 1 = (tan(s) ^ 2)
                    // TODO: // (sec(s) ^ 2) - (tan(s) ^ 2) = 1
                    // TODO: // (csc(s) ^ 2) - 1 = (cot(s) ^ 2)
                    // TODO: // (csc(s) ^ 2) - (cot(s) ^ 2) = 1

                    break;

                case DIV:
                    if(numericEquals(tree.right, 1)){ 
                        // (s) / (1) = s
                        tree = tree.left.copy();
                    }

                    else if(isNegative(tree.right)){ 
                        // (s) / (-a) = (s) * (-1/a) -> // Will get simplified later in case MUL
                        tree.token = MUL; 
                        Node a = tree.right.copy();
                        negate(a);
                        tree.right = new Node(DIV, new Node(-1), a);
                    }

                    // TODO: idk
                    // else if(){ 
                    //     // (s * a) / t = a * (s / t)
                    // }else if(){ 
                    //     // (a * s) / t = a * (s / t)
                    // }
                    
                    else if(numericEquals(tree.left, 0)){ 
                        // (0) / (s) = 0
                        tree = new Node(0);
                    }
                    
                    else if (tree.left.equals(tree.right)){ 
                        // (s) / (s) = 1
                        tree = new Node(1);
                    }

                    else if (tree.right.token.equals(POW) && tree.left.equals(tree.right.left)){ 
                        // s / (s ^ a) = s ^ (1 - a)
                        tree.token = POW;
                        tree.right.token = SUB;
                        tree.right.left = new Node(1);
                    }

                    else if (tree.left.token.equals(POW) && tree.right.equals(tree.left.left)){ 
                        // (s ^ a) / s = s ^ (a - 1)
                        Node s = tree.right.copy();
                        Node a = tree.left.right.copy();
                        tree = new Node(POW, s, new Node(SUB, a, new Node(1)));
                    }

                    else if(tree.left.token.equals(POW) && tree.right.token.equals(POW) && tree.left.left.equals(tree.right.left)){
                        // (s ^ a) / (s ^ b) = s ^ (a - b)
                        Node s = tree.left.left.copy();
                        Node a = tree.left.right.copy();
                        Node b = tree.right.right.copy();
                        tree = new Node(POW, s, new Node(SUB, a, b));
                    }

                    else if(tree.right.token.equals(DIV)){
                        // s / (a / b) = (s * b) / a
                        Node s = tree.left.copy();
                        Node a = tree.right.left.copy();
                        Node b = tree.right.right.copy();
                        tree.left = new Node(MUL, s, b);
                        tree.right = a;
                    }

                    else if(tree.left.token.equals(DIV)){
                        // (a / b) / s = a / (b * s)
                        Node a = tree.left.left.copy();
                        Node b = tree.left.right.copy();
                        Node s = tree.right.copy();
                        tree.left = a;
                        tree.right = new Node(MUL, b, s);
                    }


                    // TRIG IDENTITIES
                    else if (tree.left.token.equals(SIN) && tree.right.token.equals(COS) && tree.left.left.equals(tree.right.left)){ // sin(s) / cos(s) = tan(s)
                        Node arg = tree.left.left.copy();
                        tree = new Node(TAN);
                        tree.left = arg;
                    }else if (tree.left.token.equals(COS) && tree.right.token.equals(SIN) && tree.left.left.equals(tree.right.left)){ // cos(s) / sin(s) = cot(s)
                        Node arg = tree.left.left.copy();
                        tree = new Node(COT);
                        tree.left = arg;
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(SIN)){ // 1 / sin(s) = csc(s)
                        Node arg = tree.right.left.copy();
                        tree = new Node(CSC);
                        tree.left = arg;
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(COS)){ // 1 / cos(s) = sec(s)
                        Node arg = tree.right.left.copy();
                        tree = new Node(SEC);
                        tree.left = arg;
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(TAN)){ // 1 / tan(s) = cot(s)
                        Node arg = tree.right.left.copy();
                        tree = new Node(COT);
                        tree.left = arg;
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(CSC)){ // 1 / csc(s) = sin(s)
                        Node arg = tree.right.left.copy();
                        tree = new Node(SIN);
                        tree.left = arg;
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(SEC)){ // 1 / sec(s) = cos(s)
                        Node arg = tree.right.left.copy();
                        tree = new Node(COS);
                        tree.left = arg;
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(COT)){ // 1 / cot(s) = tan(s)
                        Node arg = tree.right.left.copy();
                        tree = new Node(TAN);
                        tree.left = arg;
                    }

                    break;

                case ADD:
                    if(numericEquals(tree.left, 0)){ 
                        // (0) + (s) = (s)
                        tree = tree.right.copy();
                    }

                    else if (tree.right.equals(tree.left)){ 
                        // (s) + (s) = 2 * s
                        tree.token = MUL;
                        tree.left = new Node(2);
                    }
                    
                    else if(tree.right.token.equals(MUL) && isNegative(tree.right.left)){
                        // s + (-a * t) = s - (a * t)
                        tree.token = SUB;
                        negate(tree.right.left);
                    }else if(tree.right.token.equals(MUL) && isNegative(tree.right.right)){
                        // s + (t * -a) = s - (t * a)
                        tree.token = SUB;
                        negate(tree.right.right);
                    }
                    
                    else if (isNegative(tree.right)){
                        // s + (-a) = s - a
                        tree.token = SUB;
                        negate(tree.right);
                    }
                    
                    else if(tree.right.token.equals(MUL) && tree.left.equals(tree.right.right)){
                        // s + (a * s) = s * (a + 1)
                        tree.token = MUL;
                        tree.right.token = ADD;
                        tree.right.right = new Node(1);
                    }else if(tree.right.token.equals(MUL) && tree.left.equals(tree.right.left)){
                        // s + (s * a) = s * (1 + a)
                        tree.token = MUL;
                        tree.right.token = ADD;
                        tree.right.left = new Node(1);
                    }
                    
                    else if(tree.right.token.equals(DIV) && tree.left.equals(tree.right.left)){
                        // s + (s / a) = s * (1 + 1 / a)
                        Node a = tree.right.right.copy();
                        tree.token = MUL;
                        tree.right.token = ADD;
                        tree.right.left = new Node(1);
                        tree.right.right= new Node(DIV, new Node(1), a);
                    }
                    
                    else if(tree.left.token.equals(MUL) && tree.right.token.equals(MUL)){
                        if(tree.left.right.equals(tree.right.right)){
                            // (a * s) + (b * s) = s * (b + a)
                            Node s = tree.left.right.copy();
                            Node a = tree.left.left.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = ADD;
                            tree.right.right = a;
                        }else if(tree.left.left.equals(tree.right.right)){
                            // (s * a) + (b * s) = s * (b + a)
                            Node s = tree.left.left.copy();
                            Node a = tree.left.right.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = ADD;
                            tree.right.right = a;
                        }else if(tree.left.right.equals(tree.right.left)){
                            // (a * s) + (s * b) = s * (a + b)
                            Node s = tree.left.right.copy();
                            Node a = tree.left.left.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = ADD;
                            tree.right.left = a;
                        }else if(tree.left.left.equals(tree.right.left)){
                            // (s * a) + (s * b) = s * (a + b)
                            Node s = tree.left.left.copy();
                            Node a = tree.left.right.copy();

                            tree.token = MUL;
                            tree.left = s;
                            tree.right.token = ADD;
                            tree.right.left = a;
                        }
                    }

                    else if(tree.left.token.equals(DIV) && tree.right.token.equals(DIV) && tree.left.right.equals(tree.right.right)){
                        // (a / s) + (b / s) = (a + b) / s
                        Node a = tree.left.left.copy();
                        Node b = tree.right.left.copy();
                        Node s = tree.left.right.copy();
                        tree = new Node(DIV, new Node(ADD, a, b), s);
                    }

                    // TRIG IDENTITIES
                    else if (tree.left.token.equals(POW) && tree.left.left.token.equals(SIN) && numericEquals(tree.left.right, 2) && tree.right.token.equals(POW) && tree.right.left.token.equals(COS) && numericEquals(tree.right.right, 2) && tree.left.left.left.equals(tree.right.left.left)){
                        // (sin(s) ^ 2) + (cos(s) ^ 2) = 1
                        tree = new Node(1);
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(POW) && tree.right.left.token.equals(TAN) && numericEquals(tree.right.right, 2)){
                        // 1 + (tan(s) ^ 2) = (sec(s) ^ 2)
                        Node arg = tree.right.left.left.copy();
                        tree = new Node(POW);
                        tree.left = new Node(SEC);
                        tree.right = new Node(2);
                        tree.left.left = arg;
                    }else if (numericEquals(tree.left, 1) && tree.right.token.equals(POW) && tree.right.left.token.equals(COT) && numericEquals(tree.right.right, 2)){
                        // 1 + (cot(s) ^ 2) = (csc(s) ^ 2)
                        Node arg = tree.right.left.left.copy();
                        tree = new Node(POW);
                        tree.left = new Node(CSC);
                        tree.right = new Node(2);
                        tree.left.left = arg;
                    }
                    
                    else{ // Addition is commutative, try swapping the subtrees
                        if(!onSubtreeSwap){
                            Node original = tree.copy();
                            tree.swapSubtrees();
                            Node newer = recSimplify(original, true, false, original);

                            if(!tree.equals(newer)) // Tree did not change, so can swap back
                                tree = newer;
                            else
                                tree.swapSubtrees();
                        }

                        // subtreesSwapped = true;
                    }

                    break;

                case POW:
                    if(numericEquals(tree.right, 0)){ // (s) ^ (0) = 1
                        tree = new Node(1);
                    }
                    
                    else if (numericEquals(tree.right, 1)){ // (s) ^ (1) = s
                        tree = tree.left.copy();
                    }
                    
                    else if (tree.left.token.equals(SQRT) && numericEquals(tree.right, 2)){
                        // sqrt(s) ^ 2 = s
                        Node arg = tree.left.left.copy();
                        tree = arg;
                    }
                    
                    else if (tree.left.token.equals(E) && tree.right.token.equals(LN)){
                        // e ^ ln(s) = s
                        Node arg = tree.right.left.copy();
                        tree = arg;
                    }
                    
                    // TODO: check for fractional exponents -> abs
                    else if (tree.left.token.equals(POW)){
                        // (s ^ a) ^ b = s ^ (a * b)
                        Node s = tree.left.left.copy();
                        Node a = tree.left.right.copy();
                        Node b = tree.right.copy();
                        tree.left = s;
                        tree.right = new Node(MUL, a, b);
                    }
                    
                    else if (isConstant(tree.left) && tree.right.token.equals(DIV) && tree.right.left.token.equals(LN) && ((tree.right.right.token.equals(LN) && tree.right.right.left.equals(tree.left)) || (numericEquals(tree.right.right, Math.log(parseNumber(tree.left.token)))))){
                        // a ^ (ln(s) / ln(a)) = s
                        tree = tree.right.left.left.copy();
                    }

                    break;

            }

            // // Tree may have been shortened during the above steps,
            // // need to check if it has children again
            // if(tree.hasLeftChild()){
            //     tree.left = recSimplify(tree.left.copy(), false, tree.left.copy());
            //     // if(subtreesSwapped) tree.swapSubtrees(); // swap back

            //     // tree.print();
            // }

            // if(tree.hasRightChild()){
            //     tree.right = recSimplify(tree.right.copy(), false, tree.right.copy());
            //     // if(subtreesSwapped) tree.swapSubtrees(); // swap back

            //     // tree.print();
            // }

            // if(subtreesSwapped) tree.swapSubtrees(); // swap back

        }else if (tree.hasLeftChild()){ // Is a function
            /* Function simplifications */
            switch(tree.token){
                case SQRT:
                    if(tree.left.token.equals(POW) && numericEquals(tree.left.right, 2)){
                        // sqrt(s ^ 2) = abs(s)
                        Node arg = tree.left.left.copy();
                        tree = new Node(ABS, arg, null);
                    }
                    break;
                case LN:
                    if(tree.left.token.equals(E)){
                        // ln (e) = 1
                        tree = new Node(1);
                    }else if(tree.left.token.equals(POW)){
                        // ln (s ^ a) = a * ln(s)
                        Node s = tree.left.left.copy();
                        Node a = tree.left.right.copy();
                        tree = new Node(MUL, a, new Node(LN, s, null));
                    }
                    // TODO: ln (e * s) = 1 + ln(s)
                    // TODO: ln (s * e) = 1 + ln(s)
                    // TODO: ln (s / e) = ln(s) - 1
                    // TODO: ln (e / s) = 1 - ln(s)
                    break;
                case ABS:
                    if(isNegative(tree.left)){
                        // abs (-s) = abs (s)
                        negate(tree.left);
                    }else if (isNumber(tree.left)){
                        // abs (-3) = 3
                        // abs (3) = 3
                        if(isNumericNegative(tree.left)) negate(tree);
                        tree = tree.left.copy();
                    }else if (tree.left.token.equals(MUL)){
                        if(numericEquals(tree.left.left, -1)){
                            // abs (-1 * s) = abs (s)
                            tree.left = tree.left.right.copy();
                        }else if (numericEquals(tree.left.right, -1)){
                            // abs (s * -1) = abs (s)
                            tree.left = tree.left.left.copy();
                        }
                    }
                    break;
                case SIN: // sin(-1 * s) = -1 * sin(s), same for csc, tan, cot
                case CSC:
                case TAN:
                case COT:
                    if(tree.left.token.equals(MUL) && isNegative(tree.left.left)){
                        // tan(-a * s) = -1 * tan(a * s)
                        negate(tree.left.left);
                        Node copy = tree.copy();
                        tree = new Node(MUL, new Node(-1), copy);
                    }else if(tree.left.token.equals(MUL) && numericEquals(tree.left.right, -1)){
                        // tan(s * -a) = -1 * tan(a * s)
                        negate(tree.left.right);
                        Node copy = tree.copy();
                        tree = new Node(MUL, new Node(-1), copy);
                    }
                    break;
                case COS: // cos(-1 * s) = cos(s), same for sec
                case SEC: 
                    if(tree.left.token.equals(MUL) && isNegative(tree.left.left)){
                        // cos(-a * s) = cos(a * s)
                        negate(tree.left.left);
                    }else if(tree.left.token.equals(MUL) && numericEquals(tree.left.right, -1)){
                        // cos(s * -a) = cos(a * s)
                        negate(tree.left.right);
                    }
                    break;
            }
            
        }

        if(onSubtreeSwap) return tree;

        // Tree may have been shortened during the above steps,
        // need to check if it has children again
        if(tree.hasLeftChild()){
            tree.left = recSimplify(tree.left.copy(), false, false, tree.left.copy());
            // tree.print();
        }

        if(tree.hasRightChild()){
            tree.right = recSimplify(tree.right.copy(), false, false, tree.right.copy());
            // tree.print();
        }

        Node copy = tree.copy();
        if(onSimplify || copy.equals(previous.copy())) return copy;
        else return recSimplify(copy.copy(), false, true, copy);

        // return tree;
    }

    
    // public Node[] treePermutations(Node tree){
    //     int numLeaves = numLeaves(tree, 0);
    // }

    public ArrayList<Node> getLeaves(Node tree, ArrayList<Node> leaves){
        if(tree != null){
            if(isConstant(tree) || isVariable(tree)){
                leaves.add(tree.copy());
            }else{
                if(tree.hasLeftChild())
                leaves = getLeaves(tree.left.copy(), leaves);
    
                if(tree.hasRightChild())
                leaves = getLeaves(tree.right.copy(), leaves);
    
                return leaves;
            }
        }

        return leaves;
    }

    // Simplifies all nodes that evaluate to a CONSTANT to that CONSTANT.
    public Node evaluateAllConstantNodes(Node tree){
        if(tree != null){
            if(evaluatesToConstant(tree)){
                return evaluateConstantNode(tree);
            }else{
                if(tree.hasLeftChild())
                    tree.left = evaluateAllConstantNodes(tree.left.copy());
    
                if(tree.hasRightChild())
                    tree.right = evaluateAllConstantNodes(tree.right.copy());
    
                return tree;
            }
        }

        return tree;
    }

    // Negates the sign of the CONSTANT contained in tree
    private void negate(Node tree) {
        // assert isNumber(tree);
        if(isNegative(tree)){
            tree.token = tree.token.substring(1);
        }else{
            tree.token = "-" + tree.token;
        }
    }

    // Retuns true if the Node is a NUMBER and if the number is negative
    public boolean isNumericNegative(Node tree) {
        return isNumber(tree) && tree.token.startsWith("-");
    }

    // More general case of the above method, returns true if the tree is a negative CONSTANT
    private boolean isNegative(Node tree){
        return isConstant(tree) && tree.token.startsWith("-");
    }

    // Returns true if the Node is a NUMBER and if it is equal to val
    public boolean numericEquals(Node tree, double val){
        return isNumber(tree) && val == parseNumber(tree.token);
    }

    // Returns true if the Node is an OPERATOR (ex. "+")
    private boolean isOperator(String token) {
        for(String operator : OPERATORS){
            if(token.equals(operator)) return true;
        }
        return false;
    }

    // Returns true if the String is a function (ex. "cos")
    private boolean isFunction(String token) {
        for(String function : FUNCTIONS){
            if(token.equals(function)) return true;
        }
        return false;
    }
    
    // Returns true if the String is a CONSTANT (ex. "a" or "12") or a VARIABLE (ex. "x")
    private boolean isConstantOrVariable(String token) {
        return !isOperator(token) && !isFunction(token) && !token.equals(OPEN_PAREN) && !token.equals(CLOSE_PAREN);
    }

    // Returns true if the String is a variable (ex. "x")
    private boolean isVariable(String token) {
        return token.equals(wrt);
    }

    // Returns true if the Node is a variable (ex. "x")
    private boolean isVariable(Node tree){
        return isVariable(tree.token);
    }
    
    // Returns true if the Node is a LEAF (aka a CONSTANT (ex. "a" or "12"))
    private boolean isConstant(Node tree){ // AKA isConstant(Node tree)
        return isConstant(tree.token);
    }
    
    // Returns true if the String is a CONSTANT (ex. "a" or "12")
    private boolean isConstant(String token){ // AKA isLeaf(String token)
        return isConstantOrVariable(token) && !isVariable(token);
    }

    private boolean isNumber(Node tree){
        return isNumber(tree.token);
    }

    // Returns true if the String is a NUMBER (ex. "-12")
    private boolean isNumber(String token){
        for(int i = 1; i < token.length(); i++){ // Start at 1 to skip the "-" sign, if one exists
            if(!Character.isDigit(token.charAt(i)) && token.charAt(i) != '.'){
                return false;
            }
        }

        return isConstant(token) && (token.startsWith("-") || Character.isDigit(token.charAt(0)));
    }

    // Returns true if the String is a LETTERED CONSTANT (ex. "a")
    private boolean isLetteredConstant(String token){
        for(int i = 0; i < token.length(); i++){
            if(!Character.isLetter(token.charAt(i))){
                return false;
            }
        }

        return isConstant(token);
    }

    // Returns true if the token is a PREDEFINED CONSTANT (ex. "pi" or "e")
    private boolean isPredefinedConstant(String token){
        for(String constant : CONSTANTS){
            if(token.equals(constant)) return true;
        }

        return false;
    }

    // Returns true if the node contains no variables -> therefore, it must evaluate to a constant
    private boolean evaluatesToConstant(Node root) {
        if (root == null) {
            return true;
        } else if (isVariable(root.token)){
            return false;
        } else {
            return evaluatesToConstant(root.left) && evaluatesToConstant(root.right);
        }
    }

    // Returns true if the token is a commutative operator, i.e ADD or MUL
    private boolean isCommutativeOperator(String token){
        return token.equals(ADD) || token.equals(MUL);
    }

    // If at least one of the node tree's children is a number, this method sets that number as the left child
    private Node bringNumberToFront(Node tree){
        if(tree.right != null && isConstant(tree.right)){
            if(isCommutativeOperator(tree.token)){
                // (x +* 3) = (3 +* x)
                tree.swapSubtrees();
            }
            
            else if (tree.token.equals(SUB)){
                // (x - 3) = (-3 + x)
                tree.swapSubtrees();
                tree.token = ADD;
                negate(tree.left);
            }else if(tree.token.equals(DIV) && !isNumber(tree.left)){
                // (x / 3) = (1/3 * x)
                Node num = tree.right.copy();
                tree.swapSubtrees();
                tree.token = MUL;
                tree.left = new Node(DIV, new Node(1), num);
            }
        }

        return tree;
    }

    // Applies the above method to all nodes in the tree
    public Node bringAllNumbersToFront(Node tree){
        if(tree != null){

            tree = bringNumberToFront(tree);

            if(tree.hasLeftChild())
                tree.left = bringAllNumbersToFront(tree.left.copy());

            if(tree.hasRightChild())
                tree.right = bringAllNumbersToFront(tree.right.copy());

            return tree;

        }

        return tree;
    }

    // TODO: This method
    private Node consolidateNumbers(Node tree){
        if(tree != null){
            tree = bringAllNumbersToFront(tree);
            // String allOperatorStrings = " ***/*///** +++-+---++ "; // De Brujin sequence of length 3 of "/*" followed by "+-"
            String mul3add3 = " *** +++ ";
            if(isCommutativeOperator(tree.token) && tree.has2Children()){
                if(isCommutativeOperator(tree.left.token) && isCommutativeOperator(tree.right.token) && mul3add3.contains(tree.token + tree.left.token + tree.right.token)){
                    // (a +* s) +* (b +* t) = (a +* b) +* (s +* t)
                    if(isNumber(tree.left.left) && isNegative(tree.right.left)){
                        Node b = tree.right.left.copy();
                        Node s = tree.left.right.copy();
                        tree.left.right = b;
                        tree.right.left = s;
                    }
                }else if (isNumber(tree.left) && isCommutativeOperator(tree.right.token) && mul3add3.contains(tree.token + tree.right.token)){
                    // a +* (b +* s) = (a +* b) + s
                    Node a = tree.left.copy();
                    Node b = tree.right.left.copy();
                    Node s = tree.right.right.copy();
                    tree = new Node(tree.token, new Node(tree.token, a, b), s);
                }else if (isNumber(tree.right) && isCommutativeOperator(tree.left.token) && mul3add3.contains(tree.token + tree.left.token)){
                    // (a +* s) +* b = (a +* b) +* s
                    Node s = tree.left.right.copy();
                    Node b = tree.right.copy();
                    tree.left.right = b;
                    tree.right = s;
                }
            }

            if(tree.hasLeftChild())
                tree.left = consolidateNumbers(tree.left.copy());

            if(tree.hasRightChild())
                tree.right = consolidateNumbers(tree.right.copy());

            return tree;

        }
        return tree;
    }

    // Pre: root contains NO variables (ex. "x"), only contains operators (ex. "+") and CONSTANTS (ex. "a" or "12")
    public Node evaluateConstantNode(Node root) {
        assert evaluatesToConstant(root) : "Node contains variables.";
        // root.print();
        String token = root.token;
        if (isNumber(token)) {
            return new Node(parseNumber(root.token));
        } else if (isConstant(root)){
            return root.copy();
        }
        
        else if (isOperator(root.token) && isNumber(root.left) && isNumber(root.right)) { // Token must be an operator
            double leftVal = parseNumber(evaluateConstantNode(root.left.copy()).token);
            if (isOperator(token)) {
                double rightVal = parseNumber(evaluateConstantNode(root.right.copy()).token);

                switch (token) {
                    case MUL:
                        return new Node(leftVal * rightVal);
                    case DIV:
                        return new Node(leftVal / rightVal);
                    case ADD:
                        return new Node(leftVal + rightVal);
                    case SUB:
                        return new Node(leftVal - rightVal);
                    case POW:
                        return new Node(Math.pow(leftVal, rightVal));
                }
            } else if (isFunction(token)) {
                // Arguments of functions are stored as the left child
                switch (token) {
                    case SIN:
                        return new Node(Math.sin(leftVal));
                    case CSC:
                        return new Node(1.0 / Math.sin(leftVal));
                    case COS:
                        return new Node(Math.cos(leftVal));
                    case SEC:
                        return new Node(1.0 / Math.cos(leftVal));
                    case TAN:
                        return new Node(Math.tan(leftVal));
                    case COT:
                        return new Node(1.0 / Math.tan(leftVal));
                    case LN:
                        return new Node(Math.log(leftVal));
                    case SQRT:
                        return new Node(Math.sqrt(leftVal));
                    case ABS:
                        return new Node(Math.abs(leftVal));
                }
            }
        }else if(isNumber(root.left)){
            double leftVal = parseNumber(evaluateConstantNode(root.left.copy()).token);
            root.left = new Node(leftVal);
        }else if(isOperator(root.token) && isNumber(root.right)){
            double rightVal = parseNumber(evaluateConstantNode(root.right.copy()).token);
            root.right = new Node(rightVal);
        }

        return root;
    }

    // Adapted from: https://www.youtube.com/watch?v=WHs-wSo33MM&
    public void generateExpressionTree() {
        Stack<Node> expressionStack = new Stack<>();

        for (String token : postfixArray) {
            if (isOperator(token)) {
                Node arg2 = expressionStack.pop();
                Node arg1 = expressionStack.pop();

                Node newTree = new Node(token);
                newTree.right = arg2;
                newTree.left = arg1;

                expressionStack.push(newTree);
            } else if (isFunction(token)) {
                Node arg = expressionStack.pop();

                Node newTree = new Node(token);
                newTree.left = arg;

                expressionStack.push(newTree);
            } else { // Must be a constant or variable
                expressionStack.push(new Node(token));
            }
        }

        originalExpressionTree = expressionStack.pop();
    }

    // Shunting yard algorithm
    // Pseudocode adapted from: https://en.wikipedia.org/wiki/Shunting-yard_algorithm
    public void convertToPostfix() {
        Stack<String> operatorStack = new Stack<>();
        String postfix = "";

        for (String token : infixArray) {
            // System.out.println(token);

            if (isOperator(token)) { // is an operator
                while (!operatorStack.empty() && (isOperator(operatorStack.peek()))
                        && ((operatorPrecedence(operatorStack.peek()) > operatorPrecedence(token))
                                || (((operatorPrecedence(operatorStack.peek()) == operatorPrecedence(token))
                                        && (operatorPrecedence(token) == 2 || operatorPrecedence(token) == 3)))
                                        && (!operatorStack.peek().equals(OPEN_PAREN)))) {
                    postfix += operatorStack.pop() + " ";
                }
                operatorStack.push(token);
            } else if (isFunction(token)) { // is a function
                operatorStack.push(token);
            } else if (token.equals(OPEN_PAREN)) { // is an opening paren
                operatorStack.push(token);
            } else if (token.equals(CLOSE_PAREN)) { // is a closing paren
                while (!operatorStack.peek().equals(OPEN_PAREN)) {
                    postfix += operatorStack.pop() + " ";
                }
                if (!operatorStack.isEmpty() && operatorStack.peek().equals(OPEN_PAREN)) {
                    operatorStack.pop();
                }
                if(!operatorStack.isEmpty() && isFunction(operatorStack.peek())){
                    postfix += operatorStack.pop() + " ";
                }
            } else { // is a number or variable
                postfix += token + " ";
            }

        }

        while (!operatorStack.empty() && (isOperator(operatorStack.peek()) || isFunction(operatorStack.peek()))) {
            postfix += operatorStack.pop() + " ";
        }

        postfixArray = postfix.split(" ");
    }

    // Pre: String operator must be an operator
    private int operatorPrecedence(String operator) {
        switch (operator) {
            case POW:
                return 4;
            case MUL:
            case DIV:
                return 3;
            case ADD:
            case SUB:
                return 2;
            default:
                return -1;
        }
    }

    // Pseudocode adapted from: https://en.wikipedia.org/wiki/Binary_expression_tree#Infix_traversal
    public void printTreeInfix(Node tree){
        if(tree != null){
            if(tree.token.endsWith(".0")) tree.token = tree.token.substring(0, tree.token.length() - 2); // For readability
            if(isOperator(tree.token)){
                System.out.print("( ");
            }else if (isFunction(tree.token)){
                System.out.print(tree.token + " ( ");
            }
            printTreeInfix(tree.left);
            if(!isFunction(tree.token))
                System.out.print(tree.token + " ");
            printTreeInfix(tree.right);
            if(isOperator(tree.token) || isFunction(tree.token)){
                System.out.print(") ");
            }
        }
    }

    public double evaluateExpression() {
        Stack<String> evalStack = new Stack<>();

        for (String token : postfixArray) {
            if (isOperator(token)) {
                double arg2 = parseNumber(evalStack.pop());
                double arg1 = parseNumber(evalStack.pop());
                double subResult = Double.NaN;

                switch (token) {
                    case MUL:
                        subResult = arg1 * arg2;
                        break;
                    case DIV:
                        subResult = arg1 / arg2;
                        break;
                    case ADD:
                        subResult = arg1 + arg2;
                        break;
                    case SUB:
                        subResult = arg1 - arg2;
                        break;
                    case POW:
                        subResult = Math.pow(arg1, arg2);
                        break;
                }

                evalStack.push("" + subResult);
            } else if (isFunction(token)) {
                double arg = parseNumber(evalStack.pop());
                double subResult = Double.NaN;

                switch (token) {
                    case SIN:
                        subResult = Math.sin(arg);
                        break;
                    case CSC:
                        subResult = 1.0 / Math.sin(arg);
                        break;
                    case COS:
                        subResult = Math.cos(arg);
                        break;
                    case SEC:
                        subResult = 1.0 / Math.cos(arg);
                        break;
                    case TAN:
                        subResult = Math.tan(arg);
                        break;
                    case COT:
                        subResult = 1.0 / Math.tan(arg);
                        break;
                    case LN:
                        subResult = Math.log(arg);
                        break;
                    case SQRT:
                        subResult = Math.sqrt(arg);
                        break;
                    case ABS:
                        subResult = Math.abs(arg);
                        break;
                }

                evalStack.push("" + subResult);

            } else { // Must be a number or variable
                evalStack.push(token);
            }

        }

        return Double.parseDouble(evalStack.pop());
    }

    private double parseNumber(String number) {
        return Double.parseDouble(number);
    }

}