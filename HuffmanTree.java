import java.io.IOException;
import java.util.HashMap;

public class HuffmanTree {
	
	// Priority queue that holds the the arrangements of nodes
	private PriorityQueue que;

	// TreeNode used to hold root of a tree
	private TreeNode root;
	
	// Variables to determine what tells to go left or right when decoding
	private final int GO_LEFT = 0;
	private final int GO_RIGHT = 1;

	// Amount of bits to write for any give node
	private final int BITS_PER_NODE = 1;

	// Representation of internal node
	private final int INTERNAL_NODE = 0;

	// Representation of leaf node
	private final int LEAF_NODE = 1;

	// Amount to add for leaf nodes for each EOF marker
	private final int ADDED_EOF = 1;

	// Number of children each node can have
	private final int NUM_OF_CHILDREN = 2; 
	
	/**
	 * Constructor used by preprocessCompress and uncompress method that
	 * helps in creating tree if we don't know structure of Huffman Tree
	 * @param que is the priority queue with the arranged characters with their frequencies
	 */
	public HuffmanTree(PriorityQueue que) {
		this.que = que;
	}
	
	/**
	 * Constructor used by compress and uncompress method that only helps
	 * in recreating tree as the structure already exists
	 * @param root is the root of a tree already made
	 */
	public HuffmanTree(TreeNode root) {
		this.root = root;
	}
	
	/**
	 * Creates a Tree of TreeNodes based on their place in the PriorityQueue
	 * Returns last TreeNode that acts as the root
	 * @param que is the priority queue holding all the TreeNodes
	 */
	public TreeNode createTree() {
		// Enters loop if there are more than one nodes that can be grouped under a parent
		while(que.size() > 1) {
			// Dequeue first two nodes which will be children
			TreeNode left = que.dequeue();
			TreeNode right = que.dequeue();
			
			// Creates a parent node with new left and right children and adds to priority queue
			TreeNode parent = new TreeNode(left, Integer.MAX_VALUE, right);
			que.enqueue(parent);
		}
		// Set the remaining TreeNode as the root for this HuffmanTree
		root = que.front();
		// Returns remaining node in queue
		return que.front();
	}
	
	/** 
	 * Returns map of character as int and their path along TreeNode root
	 * @param n is the root node of their respective tree containing characters as ints in leaves
	 * pre: n != null
	 */
	public HashMap<Integer, String> travelTree(TreeNode n) {
		// Check precondition, n must not be null
		if(n == null) {
			throw new IllegalArgumentException("Root node is null");
		}
		// Variable to form path of characters in tree
		StringBuilder s = new StringBuilder();
		HashMap<Integer, String> en = new HashMap<Integer, String>();
		
		// Traverse down tree, find paths to characters, and store into map
		travelHelper(en, s, n);
		return en;
	}
	
	/**
	 * Traverses through tree and adds path to map once we arrive at leaf node
	 * @param en is the map to which we are adding paths to
	 * @param s is the variable to keep track of path as we traves tree
	 * @param n is the node that determines our current spot
	 */
	private void travelHelper(HashMap<Integer, String> en, StringBuilder s, TreeNode n) {
		// We are at a leaf node and add the character as an int along with their current path to the map
		if(n.getLeft() == null && n.getRight() == null) {
			en.put(n.getValue(), s.toString());
			
		// We have at least one child and must check if we can traverse either side
		} else {
			// There is a left child, so add "0" to string and enter left child and then backtrack
			if(n.getLeft() != null) {
				travelHelper(en, s.append(0), n.getLeft());
				s.delete(s.length() - 1, s.length());
			}
			
			// There is a right child, so add "0" to string and enter right child and then backtrack
			if(n.getRight() != null) {
				travelHelper(en, s.append(1), n.getRight());
				s.delete(s.length() - 1, s.length());
			}
		}
	}
	
	/**
     * Traverses through tree and gets the size of the tree in bits
     * Each leaf node will have a number of bits equal to the constant BITS_PER_WORD for 
     * the character it stores plus 2 for the node itself and the EOF Marker.
     * The rest of the internal nodes will only add 1 to the bitAfter counter
     * @param n is the root node of our tree
     */
	public int countTreeInBits(TreeNode n) {
		// Return 0 since no bits will be written from this place
        if(n == null){
            return 0;
        }
        // We are at a leaf, so return the number of bits in character and 2 for the
        // node and EOF marker
        if(n.isLeaf()){
            return IHuffConstants.BITS_PER_WORD + ADDED_EOF + BITS_PER_NODE;
        }
        // Continue down tree, but return 1 for the current internal node
        return BITS_PER_NODE + countTreeInBits(n.getLeft()) + countTreeInBits(n.getRight());
	}
	
	/*
	 * Return the size of the tree
	 */
	public int getSize() {
		return countTreeInBits(root);
	}
	
	/**
	 * Write the structure of the tree using root
	 * @param bot is used to write in data for output
	 */
	public int writeTree(BitOutputStream bot) {	
		return writeTreeHelper(root, bot);
	}
	
	/**
	 * Helper method that writes the structure of the tree recursively
	 * Returns size of tree for debugging
	 * @param n is the node we are currently in
	 * @param bot is used to write in data for output
	 */
	private int writeTreeHelper(TreeNode n, BitOutputStream bot) {
		// Base case where we are at a leaf node and get data of character in node
		if(n.isLeaf()) {
			bot.writeBits(BITS_PER_NODE, LEAF_NODE);
			bot.writeBits(IHuffConstants.BITS_PER_WORD + ADDED_EOF, n.getValue());
			return IHuffConstants.BITS_PER_WORD + 2;
		}
		// Write in 1 bit for the internal node
		bot.writeBits(BITS_PER_NODE, INTERNAL_NODE);
		
		int sum = 1;
		
		// Travel left and/or right if a child exist
		if(n.getLeft() != null) {
			sum += writeTreeHelper(n.getLeft(), bot);
		}
		if(n.getRight() != null) {
			sum += writeTreeHelper(n.getRight(), bot);
		}
		return sum;
	}
	
	/**
	 * Uses input of BitInputStream to create tree based on rules set up
	 * by Huffman procedure
	 * @param bit is used to read the input of the data
	 */
	public void recreateTree(BitInputStream bit) throws IOException {
		recreateTreeHelper(root, bit);
	}
	
	/**
	 * Helper method that travels down and simultaneously creates tree based on 
	 * the next number in the BitInputStream
	 * @param n is the current node we are in
	 * @param bit is used to read the input of the data
	 */
	private void recreateTreeHelper(TreeNode n, BitInputStream bit) throws IOException {
		// Variable to place in the frequency when creating a TreeNode since it will 
		// not be useful in decoding data
		int arbitraryFreq = 0;
		
		// Reads a bit NUM_OF_CHILDREN times to check each side in traversal order
    	for(int i = 0; i < NUM_OF_CHILDREN; i++) {
    		// Get the current character in the input data
			int next = bit.readBits(BITS_PER_NODE);
			
			// If the number indicates an internal node, then traverse down tree
			if(next == INTERNAL_NODE) {
				// If the left child is null, then create an internal node and go left
				if(n.getLeft() == null) {
					n.setLeft(new TreeNode(Integer.MAX_VALUE, arbitraryFreq));
					recreateTreeHelper(n.getLeft(), bit);
					
				// If the right child is null, then create an internal node and go right
				} else {
					n.setRight(new TreeNode(Integer.MAX_VALUE, arbitraryFreq));
					recreateTreeHelper(n.getRight(), bit);
				}
				
			// If the number indicates a leaf node, then create TreeNode
			} else if(next == LEAF_NODE) {
				// Get value of the by reading the next bits holding the value of a character
				int value = bit.readBits(IHuffConstants.BITS_PER_WORD + ADDED_EOF);
				
				// If the left child is null, then create leaf node 
				if(n.getLeft() == null) {
					n.setLeft(new TreeNode(value, arbitraryFreq));
					
				// If the left child is already full, then create a leaf node in the right node 
				} else {
					n.setRight(new TreeNode(value, arbitraryFreq));
				}
			}
    	}
	}
	
	/**
	 * Write in appropriate character by following path in input and returning the 
	 * number of bits written in the process
	 * @param bit is used to read the input of the data
     * @param bot is used to write in data for output
	 */
	public int decodeData(BitInputStream bit, BitOutputStream bot) throws IOException {
    	// Variables to represent the beginning of a path
		TreeNode trav = root;
		// Variable to represent the next direction of the path
    	int next = 0;
    	// Variable to hold the number of bits written
    	int sum = 0;
    	
    	// Traverse through tree to get each character and stop once we reach the EOF marker
    	while(next != -1 && trav.getValue() != IHuffConstants.PSEUDO_EOF) {
    		// If the current node is a leaf, then write the value from the node
    		if(trav.isLeaf()) {
    			// Write value into output
    			bot.writeBits(IHuffConstants.BITS_PER_WORD, trav.getValue());
    			sum += IHuffConstants.BITS_PER_WORD;
    			// Reset the traversal node
    			trav = root;
    			
    		// We are in an internal node, check what direction to go
    		} else {
    			// Get the next direction of the input data
    			next = bit.readBits(BITS_PER_NODE);
        		
    			// Go left if the next number indicates to go left, otherwise go right
        		if(next == GO_LEFT) {
        			trav = trav.getLeft();
        		} else if(next == GO_RIGHT) {
        			trav = trav.getRight();
        		}
    		}
    	}
    	return sum;
    }
		
	/*
	 * Method to printTree for debugging purposes
	 * pre: root != null
	 */
	public void printTree(TreeNode root) {
		// Check precondition, root must not be null
		if(root == null) {
			throw new IllegalArgumentException("Root node is null");
		}
        printTree(root, "");
    }

	// Recursively print tree
    private void printTree(TreeNode n, String spaces) {
        if(n != null) {

            printTree(n.getRight(), spaces + "  ");
            System.out.println(spaces + (char)n.getValue() + " - " + n.getFrequency());
            printTree(n.getLeft(), spaces + "  ");
        }
    }
}
