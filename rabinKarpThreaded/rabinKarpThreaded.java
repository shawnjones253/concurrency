package com.company;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class rabinKarpThreaded {

    private final char[] text;
    private final int textSize;
    private int subStringLength;
    private int base;
    private int bigPrime;
    private ConcurrentMap<Integer, List<Integer>> hashes;

    public rabinKarpThreaded(String file, int subStringLength, int base, int bigPrime) throws IOException {
        this.text = readFile(file);
        this.textSize = this.text.length;
        this.subStringLength = subStringLength;
        this.base = base;
        this.bigPrime = bigPrime;
        this.hashes = new ConcurrentHashMap<>();
    }

    // getters
    public char[] getText() {
        return text;
    }
    public int getTextSize() {
        return textSize;
    }
    public int getSubStringLength() {
        return subStringLength;
    }
    public int getBase() {
        return base;
    }
    public int getBigPrime() {
        return bigPrime;
    }
    public ConcurrentMap<Integer, List<Integer>> getHashes() {
        return hashes;
    }

    // converts input file to String, replacing line separators with spaces
    private char[] readFile( String file ) throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            if ( ( line = reader.readLine() ) != null ) {
                stringBuilder.append( line );
            }
            while( ( line = reader.readLine() ) != null ) {
                stringBuilder.append( " " );
                stringBuilder.append( line );
            }

            return stringBuilder.toString().toCharArray();
        } finally {
            reader.close();
        }
    }

    // calculates a modulus b (not a remainder b)
    //  result will be positive, even if a < 0
    public int modulus(long a, long b)
    {
        return (int)((a % b + b) % b);
    }

    // code to calculate hash of a pattern
    public int computePatternHash(String patternString) {
        char[] pattern = patternString.toCharArray();

        int hashValue = 0;
        for (int i=0; i<pattern.length; i++) {
            hashValue = modulus(hashValue * base + pattern[i], bigPrime);
        }

        return hashValue;
    }

    // thread worker code
    class computeTextHashes implements Runnable {
        private int indexStart;
        private int indexEnd;
        private int textSize;
        private int subStringLength;
        private int base;
        private int bigPrime;
        private long E;

        public computeTextHashes(int start, int end) {
            this.indexStart = start;
            this.indexEnd = end;
            this.textSize = getTextSize();
            this.subStringLength = getSubStringLength();
            this.base = getBase();
            this.bigPrime = getBigPrime();
            this.E = (long)Math.pow(getBase(), getSubStringLength()-1);
        }

        public void run() {

            // check for erroneous parameters
            if (this.indexStart < 0) {
                throw new IllegalArgumentException("negative indexStart (" + this.indexStart +
                        ") found by thread " + Thread.currentThread().getId());
            }
            if (this.indexEnd >= getTextSize()) {
                throw new IllegalArgumentException("indexEnd (" + this.indexEnd +
                        ") out of range found by thread " + Thread.currentThread().getId());
            }
            if (this.subStringLength > this.textSize) {
                throw new IllegalArgumentException("subStringLength (" + this.subStringLength +
                        ") is larger than textSize (" + this.textSize + ")");
            }

            // calculate hash value of substring text[0..subStringLength-1]
            int hashValue = 0;
            for (int i=this.indexStart; i<(this.indexStart + this.subStringLength); i++) {
                hashValue = modulus(hashValue * base + text[i], bigPrime);
            }
            // add hash value of first substring to hash map
            List<Integer> valueList = Collections.synchronizedList(new ArrayList<>());
            valueList.add(this.indexStart);
            if (hashes.putIfAbsent(hashValue, valueList) == null) {
                // value added successfully
            } else {
                // there is already a mapping, add to the existing value list
                hashes.get(hashValue).add(this.indexStart);
            }


            // does this worker have the chunk of text at the end?
            if (this.indexEnd >= (getTextSize() - (this.subStringLength-1))) {
                // yes
                this.indexEnd = (getTextSize()-1)-(this.subStringLength-1);
            } else {
                // no -- leave indexEnd as is
            }


            // calculate the hash values for all substrings of size subStringLength
            //  and starting index i
            for (int i=this.indexStart+1; i<=this.indexEnd; i++) {
                valueList = Collections.synchronizedList(new ArrayList<>());
                valueList.add(i);

                hashValue = modulus(hashValue - modulus(text[i-1] * E, bigPrime), bigPrime);
                hashValue = modulus(hashValue * base, bigPrime);
                hashValue = modulus(hashValue + text[i+(subStringLength-1)], bigPrime);

                if (hashes.putIfAbsent(hashValue, valueList) == null) {
                    // value added successfully
                } else {
                    // there is already a mapping, add to the existing value
                    hashes.get(hashValue).add(i);
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
	    int numThreads = 8;
        String file = "lorem90k.txt";
        String pattern = "ipsum";
        int subStringLength = pattern.length();
        int base = 457;
        int bigPrime = 6131;
        rabinKarpThreaded myRK = new rabinKarpThreaded(file, subStringLength, base, bigPrime);
        int chunkSize = myRK.getTextSize() / numThreads;

        // get start time
        double startTime = System.nanoTime();

        // create threads
        try {
            ExecutorService ex = Executors.newFixedThreadPool(numThreads);
            for (int i=0; i<numThreads; i++) {
                ex.execute(myRK.new computeTextHashes(i*chunkSize, (i+1)*chunkSize - 1));
            }
            ex.shutdown();
            ex.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        // get end time
        double endTime = System.nanoTime();

        // print out time and thread information
        System.out.println(numThreads + " threads took "
                + ((endTime-startTime)/1E9) + " sec to calculate hashes.");

        // print out input size
        System.out.println("Text size: " + myRK.getTextSize() + " bytes.");

        // get hash of pattern
        int patternHash = myRK.computePatternHash(pattern);

        // print out pattern
        System.out.println("Pattern: " + pattern);

        int matches = 0;
        int spuriousHits = 0;
        String possibleMatch;
        List<Integer> matchList = new ArrayList<>();

        // iterate through matching hashes
        for (int offset : myRK.getHashes().get(patternHash)) {
            possibleMatch = String.valueOf(myRK.getText(), offset, 5);
            if (possibleMatch.equals(pattern)) {
                matches++;
                matchList.add(offset);
            } else {
                spuriousHits++;
            }
        }

        Collections.sort(matchList);
        System.out.println("Matches at indexes: " + matchList.toString());
        System.out.println("Matches: " + matches);
        System.out.println("Spurious hits: " + spuriousHits);

    }
}
