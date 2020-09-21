import java.io.IOException;
import java.util.LinkedList;

public class PriorityQueue {
	
	// Front of the queue will be the beginning of the linked list
	private LinkedList<TreeNode> con;
	// Int to represent the size of the list
	private int size;
	
	/**
	 *  Constructor that takes in array of frequencies used by preprocessCompressed
	 *  method to fill the container with appropriate TreeNodes
	 *  @param count is the array containing the frequencies of every character
	 */
	public PriorityQueue(int[] count) {
		con = new LinkedList<TreeNode>();
		size = 0;
		
		// Adding all TreeNodes with their respective frequency to priority queue
        for(int i = 0; i < count.length; i++) {
        	// Only add characters that appear in data
        	if(count[i] != 0) {
        		TreeNode insert = new TreeNode(i, count[i]);
            	enqueue(insert);
        	}
        }
        // Enqueue Pseudo_EOF value to avoid extra information
        enqueue(new TreeNode(IHuffConstants.PSEUDO_EOF, 1));
	}
	
	/**
	 *  Constructor that takes in a BitInputStream that is used to create by reading
	 *  the numbers of frequencies in input for the stored count used by uncompress method
	 *  @param bit is used to read the input of the data
	 */
	public PriorityQueue(BitInputStream bit) throws IOException {
		con = new LinkedList<TreeNode>();
		size = 0;
		
		// Traverse through the bit input reading for ALPH_SIZE to get all the
		// frequencies of the counts stored
		for(int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
			int indexFreq = bit.readBits(IHuffConstants.BITS_PER_INT);
			
			// Only add a TreeNode to queue if the frequency indicates that a
			// a character appears at least once
			if(indexFreq != 0) {
				TreeNode insert = new TreeNode(i, indexFreq);
				enqueue(insert);
			}
		}
		// Enqueue the EOF marker
		enqueue(new TreeNode(IHuffConstants.PSEUDO_EOF, 1));
	}
	
	/**
	 * Add node to list, assuming parameter is not present in list already
	 * pre = val != null
	 * @param val is node we are adding
	 */
	public void enqueue(TreeNode val) {
		// Check precondition, val must not be null
		if(val == null) {
			throw new IllegalArgumentException("Parameter node is null");
		}
		
		// If list is initially empty, val to front
		if(isEmpty()) {
			con.addFirst(val);
			
		// Traverse through linked list to find spot for val
		} else {
			int index = 0;
			boolean leave = false;
			
			// Go through queue and exit if found a spot to add val
			while(index < size && !leave) {
				// Check if val is less than current node, exit list traversal
				if(val.compareTo(con.get(index)) < 0) {
					leave = true;
				}
				index++;
			}
			// If found spot within list, add val node in index; otherwise add node to last
			if(leave) {
				con.add(index - 1, val);
			} else {
				con.addLast(val);
			}
		}
		size++;
	}
	
	/*
	 * Remove node at the front of the queue
	 * Returns reference to node removed, null if not present
	 */
	public TreeNode dequeue() {
		TreeNode de = con.getFirst();
		size--;
		con.removeFirst();
		return de;
	}
	
	/*
	 * Access node at the front of this queue
	 * pre: !isEmpty()
	 */
	public TreeNode front() {
		// Check precondition, list must be a non-empty container
		if(isEmpty()) {
			throw new IllegalStateException("List is empty.");
		}
		return con.getFirst();
	}
	
	/*
	 * Determines if list is empty
	 */
	public boolean isEmpty() {
		return size == 0;
	}
	
	/*
	 * Returns number of nodes in list
	 */
	public int size() {
		return size;
	}
		
	// Traverse through linked list to print queue for debugging
	public String toString() {
		String str = "[";
		
		for(int i = 0; i < size; i++) {
			str += con.get(i) + ", \n";
		}
		
		str = str.substring(0, str.length() - 1);
		return str + "]";
	}
}
