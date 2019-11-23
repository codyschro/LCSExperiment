import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class LCSExperiment {
    static ThreadMXBean bean = ManagementFactory.getThreadMXBean();


    /* define constants */
    static int MAXINPUTSIZE = 17000;
    static int numberOfTrials = 20;
    static String ResultsFolderPath = "/home/codyschroeder/Results/"; // pathname to results folder
    static FileWriter resultsFile;
    static PrintWriter resultsWriter;

    public static void main(String[] args) throws IOException {
        // run the whole experiment at least twice, and expect to throw away the data from the earlier runs, before java has fully optimized
        System.out.println("Running first full experiment...");
        runFullExperiment("LCSBookFaster-Exp1-ThrowAway.txt");
        System.out.println("Running second full experiment...");
        runFullExperiment("LCSBookFaster-Exp2.txt");
        System.out.println("Running third full experiment...");
        runFullExperiment("LCSBookFaster-Exp3.txt");
    }

    static void runFullExperiment(String resultsFileName) throws IOException {


        try {
            resultsFile = new FileWriter(ResultsFolderPath + resultsFileName);
            resultsWriter = new PrintWriter(resultsFile);

        } catch (Exception e) {
            System.out.println("*****!!!!!  Had a problem opening the results file " + ResultsFolderPath + resultsFileName);
            return; // not very foolproof... but we do expect to be able to create/open the file...
        }

        ThreadCpuStopWatch BatchStopwatch = new ThreadCpuStopWatch(); // for timing an entire set of trials
        ThreadCpuStopWatch TrialStopwatch = new ThreadCpuStopWatch(); // for timing an individual trial

        resultsWriter.println("#    StringLength     T(avg runtime)    Doubling Ratio"); // # marks a comment in gnuplot data
        resultsWriter.flush();

        double previousTime = -1;
        double doublingRatio = 0;
        /* for each size of input we want to test: in this case starting small and doubling the size each time */
        for (int inputSize = 1; inputSize <= MAXINPUTSIZE; inputSize *= 2) {
            // progress message...
            System.out.println("Running test for input size " + inputSize + " ... ");
            /* repeat for desired number of trials (for a specific size of input)... */
            long batchElapsedTime = 0;
            System.out.print("    Running trial batch...");
            /* force garbage collection before each batch of trials run so it is not included in the time */
            System.gc();
            // instead of timing each individual trial, we will time the entire set of trials (for a given input size)
            // and divide by the number of trials -- this reduces the impact of the amount of time it takes to call the
            // stopwatch methods themselves
            //BatchStopwatch.start(); // comment this line if timing trials individually

            // run the tirals
            for (long trial = 0; trial < numberOfTrials; trial++) {

                /* force garbage collection before each trial run so it is not included in the time */
                System.gc();
                //String s1 = justXString(inputSize);
                //String s2 = justXString(inputSize);
                //String s1 = createRandomStrings(inputSize);
                //String s2 = createRandomStrings(inputSize);
                //from here: https://javarevisited.blogspot.com/2015/09/how-to-read-file-into-string-in-java-7.html
                String book = Files.readString(Paths.get("/home/codyschroeder/Books/tomsawyer.txt"));
                String s1 = getRandomSubString(inputSize, book);
                String s2 = getRandomSubString(inputSize, book);

                TrialStopwatch.start(); // *** uncomment this line if timing trials individually
                /* run the function we're testing on the trial input */
                //String answer = LCSBruteForce(s1, s2);
                String answer = LCSFaster(s1, s2);

                batchElapsedTime = batchElapsedTime + TrialStopwatch.elapsedTime(); // *** uncomment this line if timing trials individually
            }

            //batchElapsedTime = BatchStopwatch.elapsedTime(); // *** comment this line if timing trials individually
            double averageTimePerTrialInBatch = (double) batchElapsedTime / (double) numberOfTrials; // calculate the average time per trial in this batch

            //calculate doubling ratio
            doublingRatio = averageTimePerTrialInBatch / previousTime;
            previousTime = averageTimePerTrialInBatch;

            /* print data for this size of input */
            resultsWriter.printf("%12d  %15.2f  %14f\n", inputSize, averageTimePerTrialInBatch, doublingRatio); // might as well make the columns look nice
            resultsWriter.flush();
            System.out.println(" ....done.");

        }
    }

    //implemented from video, could not get instances to work correctly... so improvised. returns correct substring tho
    public static String LCSBruteForce(String s1, String s2) {

        //initialize lengths, index trackers, answer string
        int length1 = s1.length();
        int length2 = s2.length();
        int lcsLength = 0;
        int s1Start = 0;
        int s2Start = 0;
        String answer = "";

        //use two loops to loop through possibilities
        for (int i = 0; i < length1; i++) {
            for (int j = 0; j < length2; j++) {
                //use third loop to loop over matching characters of both
                for (int k = 0; k < Math.min(length1 - i, length2 - j); k++) {
                    //if no match, break
                    if (s1.charAt(i + k) != s2.charAt(j + k))
                        break;
                    //if length of matching substrings larger than before, save length and starting indexes
                    if (k + 1 > lcsLength) {
                        lcsLength = k + 1;
                        s1Start = i;
                        s2Start = j;
                    }
                }
            }

        }
        if (lcsLength == 0) {
            answer = "No Common Substring";
            return answer;
        }
        else {
            answer = s1.substring(s1Start, s1Start + lcsLength);
            return answer;
        }
    }

    //implemented using dynamic programming from here: https://www.geeksforgeeks.org/longest-common-substring-dp-29/
    public static String LCSFaster(String s1, String s2) {

        //get lengths, set lcsLength, answer string
        int length1 = s1.length();
        int length2 = s2.length();
        int lcsLength = 0;
        String answer = "";

        //create a table to store lengths of longest common suffixes of substrings
        int[][] LCSuffix = new int[length1 + 1][length2 + 1];

        //store index of cell which has max value
        int row = 0, col = 0;

        //build 2d array from bottom up
        for (int i = 0; i <= length1; i++) {
            for (int j = 0; j <= length2; j++) {
                if (i == 0 || j == 0)
                    LCSuffix[i][j] = 0;
                else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    LCSuffix[i][j] = LCSuffix[i - 1][j - 1] + 1;
                    if (lcsLength < LCSuffix[i][j]) {
                        lcsLength = LCSuffix[i][j];
                        row = i;
                        col = j;
                    }
                }
                else
                    LCSuffix[i][j] = 0;
            }
        }
        // if true, then no common substring exists
        if (lcsLength == 0) {
            answer = "No Common Substring";
            return answer;
        }

        // traverse up diagonally form the (row, col) cell
        // until LCSuffix[row][col] != 0
        while (LCSuffix[row][col] != 0) {
            answer = s1.charAt(row - 1) + answer; // or Y[col-1]
            --lcsLength;

            // move diagonally up to previous cell
            row--;
            col--;
        }

        return answer;
    }
    public static String justXString(int size){

        StringBuilder newString = new StringBuilder(size);
        for(int i = 0; i < size; i++){
            newString.append('x');
        }

        return newString.toString();
    }

    public static String createRandomStrings(int size){

        //choose a random character from this string
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789" + "abcdefghijklmnopqrstuvxyz"
                + "!@#$%^&*()-=_+*/><,. {}|";
        //create buffer for size of string
        StringBuilder sb = new StringBuilder(size);
        //loop thru adding a character each time until size is reached
        for(int i = 0; i < size; i++){
            int index = (int)(alphaNumericString.length() * Math.random());
            sb.append(alphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    public static String getRandomSubString(int size, String s1){
        int lower = 0;
        int upper = s1.length() - size;
        //create random index
        Random random = new Random();
        int index = lower + (int)(random.nextFloat() * (upper - lower + 1));
        //return substring
        return s1.substring(index, index + size);
    }
}
