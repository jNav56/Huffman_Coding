/*  Student information for assignment:
 *
 *  On MY honor, Juan Nava, this programming assignment is MY own work
 *  and I have not provided this code to any other student.
 *
 *  Number of slip days used:0
 *
 *  Student
 *  UTEID:jcn842
 *  email address:nava.juan1012@gmail.com
 *  Grader name:Ethan
 *  
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleHuffProcessor implements IHuffProcessor {

	private IHuffViewer myViewer;
    // Map of ASCCI version as a Key and it's path on a Tree based on frequency
    private HashMap<Integer, String> newCoding;
    // Root node of tree made after priority queue is made
    private TreeNode root;
    // Array keeps count of frequencies of each character for whole class
    private int[] count;
    // Determines the header to use for format
    private int format;
    // Records the number of bits saved int the preprocessCompress method
    private int bitsSaved;
    // Boolean to keep track if the preprocessCompress method has been called yet
    private boolean preProcessCalled = false;

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     * @param in is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind of
     * header to use, standard count format, standard tree format, or
     * possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     * Note, to determine the number of
     * bits saved, the number of bits written includes
     * ALL bits that will be written including the
     * magic number, the header format number, the header to
     * reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        try {
        	BitInputStream bit = new BitInputStream(in);
        	// Change boolean to allow compression method to be called later
        	preProcessCalled = true;
        	// Initialize count array for frequencies
            count = new int[ALPH_SIZE];
            // Initialize format for determining use of compression and uncompressing
            format = headerFormat;
            
            // Adding frequencies into count array
            frequency(bit);
            
            PriorityQueue que = new PriorityQueue(count);
            HuffmanTree tree = new HuffmanTree(que);
            
            // Get tree root of resulting priority queue
            root = tree.createTree();
            // Travels tree and makes path of each characters
            newCoding = tree.travelTree(root);
            
            getBitsSaved(tree);
            return bitsSaved;
            
        } catch(Exception e) {
        	showString("Not working yet");
        	myViewer.update("Still not working");
        	throw new IOException("preprocess not implemented");
        }
    }
    
    /*
     * Returns the number of bits saved by getting the difference between the
     * number of bits before compression and after encoding
     */
    private void getBitsSaved(HuffmanTree tree) {
    	// Variables to hold bits before and after compression
    	int bitsBefore = 0;
    	int bitsAfter = 0;
    	
    	// Recurses through the count array to check for bits of original file
    	for(int i = 0; i < count.length; i++) {
    		// Get the frequency of the character and multiply it by the bits of it
			// in the original file and add it to the sum of bits before encoding
			bitsBefore += count[i] * BITS_PER_WORD;
    	}
    	
    	// Begin getting amount of bits of hf file
        // Add bits for magic number and format bit number
        bitsAfter += 2 * BITS_PER_INT;
        
        // Get number of bits if format would be SCF or STF
        bitsAfter += bitsForFormat(tree);
        // Adding bits from data written in compression method
        for(int cha: newCoding.keySet())
        	if(cha != ALPH_SIZE)
        		bitsAfter += newCoding.get(cha).length() * count[cha];
        	
        // Adding length of EOF marker since it is not in the count array
        bitsAfter += newCoding.get(PSEUDO_EOF).length();
        bitsSaved = bitsBefore - bitsAfter;
    }
    
    /**
     * Return the bits that would be written depending on the header of the file
     * @param tree is the HuffmanTree object to be used to get the size of the tree in bits
     */
    private int bitsForFormat(HuffmanTree tree) {
    	int bitsAfter = 0;
    	// Add to bitsAfter depending on the header format
        // If standard count format, just add BITS_PER_INT constant for each index on the array
        if(format == STORE_COUNTS) {
            for(int i = 0; i < ALPH_SIZE; i++)
                bitsAfter += BITS_PER_INT;
            
        // Add one instance of BITS_PER_INT constant and the following structure of the tree
        } else if(format == STORE_TREE) {
            bitsAfter += BITS_PER_INT;
            bitsAfter += tree.countTreeInBits(root);
        }
        return bitsAfter;
    }
    
    /*
     * Traverse through the input stream and add frequencies to array in respective index
     */
    private void frequency(BitInputStream bit) throws IOException {
    	int next = bit.readBits(BITS_PER_WORD);
    	// Continue as long as there are characters in file
    	while(next != -1) {
    		// Get int corresponding to character and increment index in count
    		count[next]++;
    		next = bit.readBits(BITS_PER_WORD);
    	}
    	bit.close();
    }
    
    /**
	 * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method
     * @param in is the stream being compressed (NOT a BitInputStream)
     * @param out is bound to a file/stream to which bits are written
     * for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than the input file.
     * If this is false do not create the output file if it is larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        // Checks precondition, preprocessCompress method must be called
    	if(!preProcessCalled) {
    		throw new IllegalStateException("Pre-processing method has not been called.");
    	}
        try {
        	// Record number of bits written to compress file
        	int bitsWritten = 0;
        	
        	// Compress file if bits would be saved or the user forces a compression to happen
        	if(bitsSaved > 0 || force) {
        		BitInputStream bit = new BitInputStream(in);
        		BitOutputStream bot = new BitOutputStream(out);
        		
        		// Increment bitsWritten by BITS_PER_INT twice for magic number and header format
        		bitsWritten += 2 * BITS_PER_INT;
        		bot.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        		
        		// Write the bits for the information needed to reconstruct tree and get bitsWritten
        		bitsWritten += compressBasedOnFormat(bit, bot);
        		
        		// Write the bits for the actual data and get bitsWritten
        		bitsWritten += compressDataAndEOF(bit, bot);
        		
        	// If the number of bitsSaved is less than the actual data, then let user known about force compression
        	} else if(bitsSaved <= 0) {
        		myViewer.update("Compressed file has " + (-1 * bitsSaved) + " more bits "
        				+ "than the \nuncompressed file. Select the \"Force Compression\" \n"
        				+ "option under the \"Options\" tab.");
        	}
        	return bitsWritten;
        	
        } catch (Exception e) {
        	throw new IOException("compress is not implemented");
        }
    }
    
    /**
     * Write in the information to create an array of frequencies to then create the tree
     * that is used to decode data or write data of tree structure to recreate it.
     * Return number of bits written in the process
     * @param bit is used to read the input of the data
     * @param bot is used to write in data for output
     */
    private int compressBasedOnFormat(BitInputStream bit, BitOutputStream bot) {
    	// Record number of bits written to compress file
    	int bitsWritten = 0;
    	
    	// If the header is SCF, then write data for the array of frequencies
    	if(format == STORE_COUNTS) {
			// Write header for standard count format
			bot.writeBits(BITS_PER_INT, STORE_COUNTS);
			
			// Write the frequency of each index in count array and record bits written
			for(int i = 0; i < ALPH_SIZE; i++) {
				bitsWritten += BITS_PER_INT;
				bot.writeBits(BITS_PER_INT, count[i]);
			}
			
		// If the header is STF, then write data for structure of tree
		} else if(format == STORE_TREE) {
			// Write header for standard tree format
			bot.writeBits(BITS_PER_INT, STORE_TREE);
			
			HuffmanTree tree = new HuffmanTree(root);
			
			// Write in size of tree
			int size = tree.getSize();
			bitsWritten += BITS_PER_INT + size;
			bot.writeBits(BITS_PER_INT, size);
			
			// Write in bits defining the structure of the tree
			tree.writeTree(bot);
		}
    	return bitsWritten;
    }
    
    /**
     * Write in the information of the coding paths of each character followed by the EOF
     * Return number of bits written in the process
     * @param bit is used to read the input of the data
     * @param bot is used to write in data for output
     */
    private int compressDataAndEOF(BitInputStream bit, BitOutputStream bot) throws IOException {
    	// Record number of bits written to compress file
    	int bitsWritten = 0;
    	
    	// Get ASCII value of first character
		int next = bit.readBits(BITS_PER_WORD);
		
		// Write in data as long as it exists
		while(next != -1) {
			// Get the path of the character within tree
			String path = newCoding.get(next);
			
			// Traverse through path String, write in path, and increment bitsWritten 
			for(int i = 0; i < path.length(); i++) {
				bot.writeBits(1, (path.charAt(i) == '0'? 0: 1));
			}
			bitsWritten += path.length();
			
			// Get next ASCII value of next character
			next = bit.readBits(BITS_PER_WORD);
		}
		bit.close();
		
		// Get the ASCII value of the EOF marker
		String EOF = newCoding.get(PSEUDO_EOF);
		// Traverse through path of EOF marker, write in path, and increment bitsWritten
		for(int i = 0; i < EOF.length(); i++) {
			bot.writeBits(1, (EOF.charAt(i) == '0'? 0: 1));
		}
		bitsWritten += EOF.length(); 
		bot.close();
    	return bitsWritten;
    }
    
	/**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * @param in is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {      
    	try {
    		// Record the number of bits written in newly formed file
    		int bitsWritten = 0;
    		
    		BitInputStream bit = new BitInputStream(in);
    		BitOutputStream bot = new BitOutputStream(out);
		        	
    		// Check if magic number is correct to uncompress file
    		int magic = bit.readBits(BITS_PER_INT);
    		if (magic != MAGIC_NUMBER) {
    			myViewer.showError("Error reading compressed file. \n" +
    					"File did not start with the huff magic number.");
    			bit.close();
    			bot.close();
    			return -1;
    		}
    		
    		// Get header of this uncompressed file
    		int header = bit.readBits(BITS_PER_INT);
    		// Get the HuffmanTree that contains the paths to decode data
    		HuffmanTree t = getHuffTree(bit, header);
    		// Decode data with Huffman tree and record the number of bits written
    		bitsWritten += t.decodeData(bit, bot);
    		
    		bit.close();
    		bot.close();
    		return bitsWritten;
    		
    	} catch(Exception e) {
    		throw new IOException("uncompress not implemented");	
    	}
    }
    
    /**
     * Return the reference to HuffmanTree object whose root variable will contain the
     * necessary TreeNode reference to decode uncompressed file
     * @param bit is used to read the input of the data
     * @param header is used to determine which method to use to decompress file 
     */
    private HuffmanTree getHuffTree(BitInputStream bit, int header) throws IOException {
    	HuffmanTree t = null;
    	
    	// If the header is SCF, then create tree using an array of frequencies 
    	if(header == STORE_COUNTS) {
			// Make priority queue
			PriorityQueue que = new PriorityQueue(bit);
			
			// Make tree and set root
			t = new HuffmanTree(que);
			t.createTree();
			
		// If the header is STF, then recreate tree by creating nodes based on bit reading	
		} else if(header == STORE_TREE) {
			// Retrieve size of the tree
			int size = bit.readBits(BITS_PER_INT);
			// Read first number and assume it represents the root of the tree
			bit.readBits(1);
			t = new HuffmanTree(new TreeNode(Integer.MAX_VALUE, size));
			
			// Follow the reading of the input to place nodes in pre order traversal
			t.recreateTree(bit);
		}
    	return t;
    }
       
    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s){
        if(myViewer != null)
            myViewer.update(s);
    }
}