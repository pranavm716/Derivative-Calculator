public class Node {
    String token;
    Node left;
    Node right;

    public Node(){
        token = "";
        left = right = null;
    }

    public Node(String token) {
        this.token = token;
        left = right = null;
    }

    public Node(String token, Node left, Node right){
        this.token = token;
        this.left = left;
        this.right = right;
    }

    public Node(double token){
        this.token = "" + token;
        left = right = null;
    }

    public boolean has2Children(){
        return this.left != null && this.right != null;
    }

    public boolean hasLeftChild(){
        return this.left != null;
    }

    public boolean hasRightChild(){
        return this.right != null;
    }

    public boolean isLeaf(){
        return !hasLeftChild() && !hasRightChild();
    }

    public void swapSubtrees(){
        Node lcopy = left.copy();
        Node rCopy = right.copy();
        left = rCopy;
        right = lcopy;
    }
    
    // Taken from: https://stackoverflow.com/questions/16098362/how-to-deep-copy-a-binary-tree
    public Node copy() {
        Node left = null;
        Node right = null;
        if (this.left != null) {
            left = this.left.copy();
        }
        if (this.right != null) {
            right = this.right.copy();
        }
        return new Node(token, left, right);
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: Check equality across all combinations of * and + subtree swaps
        assert obj instanceof Node;
        return recEquals(this, (Node)obj);
    }

    // Pseudocode adapted from: https://www.geeksforgeeks.org/check-whether-the-two-binary-search-trees-are-identical-or-not/
    public boolean recEquals(Node thisTree, Node anotherTree){
        if(thisTree == null && anotherTree == null){
            return true;
        }else{
            return thisTree.token.equals(anotherTree.token) && recEquals(thisTree.left, anotherTree.left) && recEquals(thisTree.right, anotherTree.right);
        }
    }
    
    // Taken from: https://www.baeldung.com/java-print-binary-tree-diagram
    private String traversePreOrder(Node root) {
        
        if (root == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(root.token);
        
        String pointerRight = "└──";
        String pointerLeft = (root.right != null) ? "├──" : "└──";
        
        traverseNodes(sb, "", pointerLeft, root.left, root.right != null);
        traverseNodes(sb, "", pointerRight, root.right, false);
        
        return sb.toString();
    }
    
    private void traverseNodes(StringBuilder sb, String padding, String pointer, Node node, boolean hasRightSibling) {
        if (node != null) {
            sb.append("\n");
            sb.append(padding);
            sb.append(pointer);
            sb.append(node.token);
            
            StringBuilder paddingBuilder = new StringBuilder(padding);
            if (hasRightSibling) {
                paddingBuilder.append("│  ");
            } else {
                paddingBuilder.append("   ");
            }
            
            String paddingForBoth = paddingBuilder.toString();
            String pointerRight = "└──";
            String pointerLeft = (node.right != null) ? "├──" : "└──";
            
            traverseNodes(sb, paddingForBoth, pointerLeft, node.left, node.right != null);
            traverseNodes(sb, paddingForBoth, pointerRight, node.right, false);
        }
    }
    
    public void print() {
        System.out.println();
        System.out.print(traversePreOrder(this));
        System.out.println();
    }

    @Override
    public String toString() {
        return String.format(traversePreOrder(this) + "\n");
    }
}