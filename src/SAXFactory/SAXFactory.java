/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SAXFactory;

import edu.hawaii.jmotif.sax.DistanceEntry;
import edu.hawaii.jmotif.sax.LargeWindowAlgorithm;
import edu.hawaii.jmotif.sax.SlidingWindowMarkerAlgorithm;
import edu.hawaii.jmotif.sax.alphabet.Alphabet;
import edu.hawaii.jmotif.sax.alphabet.NormalAlphabet;
import edu.hawaii.jmotif.sax.datastructures.DiscordRecord;
import edu.hawaii.jmotif.sax.datastructures.MotifRecord;
import edu.hawaii.jmotif.sax.datastructures.MotifRecords;
import edu.hawaii.jmotif.sax.datastructures.SAXFrequencyData;
import edu.hawaii.jmotif.sax.trie.SAXTrie;
import edu.hawaii.jmotif.sax.trie.SAXTrieHitEntry;
import edu.hawaii.jmotif.sax.trie.TrieException;
import edu.hawaii.jmotif.sax.trie.VisitRegistry;
import edu.hawaii.jmotif.timeseries.TSException;
import edu.hawaii.jmotif.timeseries.TSUtils;
import edu.hawaii.jmotif.timeseries.Timeseries;
import edu.hawaii.jmotif.util.BriefFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hackystat.utilities.logger.HackystatLogger;
import org.hackystat.utilities.stacktrace.StackTrace;
import weka.core.Attribute;
import weka.core.Instances;

/**
 * Implements SAX algorithms.
 *
 * @author Pavel Senin
 *
 */
public final class SAXFactory {

    public static final int DEFAULT_COLLECTION_SIZE = 50;

    private static Logger consoleLogger;
//    private static final String LOGGING_LEVEL = "SEVERE";
    private static final String LOGGING_LEVEL = "INFO";

    static {
        consoleLogger = HackystatLogger.getLogger("jmotif.debug.console", "jmotif");
        consoleLogger.setUseParentHandlers(false);
        for (Handler handler : consoleLogger.getHandlers()) {
            consoleLogger.removeHandler(handler);
        }
        ConsoleHandler handler = new ConsoleHandler();
        Formatter formatter = new BriefFormatter();
        handler.setFormatter(formatter);
        consoleLogger.addHandler(handler);
        HackystatLogger.setLoggingLevel(consoleLogger, LOGGING_LEVEL);
    }

    /**
     * Constructor.
     */
    private SAXFactory() {
        super();
    }

    public static void setLoggerLevel(Level level) {
        HackystatLogger.setLoggingLevel(consoleLogger, level.toString());
    }

    /**
     * Convert the timeseries into SAX string representation, normalizes each of
     * the pieces before SAX conversion. NOSKIP means that ALL SAX words
     * reported.
     *
     * @param ts The timeseries given.
     * @param windowSize The sliding window size used.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param cuts The alphabet cuts to use.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     * @throws CloneNotSupportedException
     */
    public static SAXFrequencyData ts2saxZnormByCutsNoSkip(Timeseries ts, int windowSize,
            int paaSize, double[] cuts) throws TSException, CloneNotSupportedException {

        // Initialize symbolic result data
        SAXFrequencyData res = new SAXFrequencyData();

        // scan across the time series extract sub sequences, and converting
        // them to strings
        for (int i = 0; i < ts.size() - (windowSize - 1); i++) {

            // fix the current subsection
            Timeseries subSection = ts.subsection(i, i + windowSize - 1);

            // Z normalize it
            subSection = TSUtils.zNormalize(subSection);

            // perform PAA conversion if needed
            Timeseries paa;
            try {
                paa = TSUtils.paa(subSection, paaSize);
            } catch (CloneNotSupportedException e) {
                throw new TSException("Unable to clone: " + StackTrace.toString(e));
            }

            // Convert the PAA to a string.
            char[] currentString = TSUtils.ts2StringWithNaNByCuts(paa, cuts);

            res.put(new String(currentString), i);
        }
        return res;
    }

    /**
     * Convert the timeseries into SAX string representation, normalizes each of
     * the pieces before SAX conversion. Not all SAX words reported, if the new
     * SAX word is the same as current it will not be reported.
     *
     * @param ts The timeseries given.
     * @param windowSize The sliding window size used.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param cuts The alphabet cuts to use.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     * @throws CloneNotSupportedException
     */
    public static SAXFrequencyData ts2saxZnormByCuts(Timeseries ts, int windowSize, int paaSize,
            double[] cuts) throws TSException, CloneNotSupportedException {

        // Initialize symbolic result data
        SAXFrequencyData res = new SAXFrequencyData();
        String previousString = "";

        // scan across the time series extract sub sequences, and converting
        // them to strings
        for (int i = 0; i < ts.size() - (windowSize - 1); i++) {

            // fix the current subsection
            Timeseries subSection = ts.subsection(i, i + windowSize - 1);

            // Z normalize it
            subSection = TSUtils.zNormalize(subSection);

            // perform PAA conversion if needed
            Timeseries paa;
            try {
                paa = TSUtils.paa(subSection, paaSize);
            } catch (CloneNotSupportedException e) {
                throw new TSException("Unable to clone: " + StackTrace.toString(e));
            }

            // Convert the PAA to a string.
            char[] currentString = TSUtils.ts2StringWithNaNByCuts(paa, cuts);

            // check if previous one was the same, if so, ignore that (don't
            // know why though, but guess
            // cause we didn't advance much on the timeseries itself)
            if (!previousString.isEmpty() && previousString.equalsIgnoreCase(new String(currentString))) {
                continue;
            }
            previousString = new String(currentString);
            res.put(new String(currentString), i);
        }
        return res;
    }

    /**
     * Convert the timeseries into SAX string representation, normalizes each of
     * the pieces before SAX conversion. NOSKIP means that ALL SAX words
     * reported.
     *
     * @param s The timeseries given.
     * @param windowSize The sliding window size used.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param cuts The alphabet cuts to use.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     * @throws CloneNotSupportedException
     */
    public static SAXFrequencyData ts2saxZnormByCutsNoSkip(double[] s, int windowSize, int paaSize,
            double[] cuts) throws TSException, CloneNotSupportedException {
        long[] ticks = new long[s.length];
        for (int i = 0; i < s.length; i++) {
            ticks[i] = i;
        }
        Timeseries ts = new Timeseries(s, ticks);
        return ts2saxZnormByCutsNoSkip(ts, windowSize, paaSize, cuts);
    }

    /**
     * Convert the timeseries into SAX string representation, normalizes each of
     * the pieces before SAX conversion. Not all SAX words reported, if the new
     * SAX word is the same as current it will not be reported.
     *
     * @param s The timeseries given.
     * @param windowSize The sliding window size used.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param cuts The alphabet cuts to use.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     * @throws CloneNotSupportedException
     */
    public static SAXFrequencyData ts2saxZnormByCuts(double[] s, int windowSize, int paaSize,
            double[] cuts) throws TSException, CloneNotSupportedException {
        long[] ticks = new long[s.length];
        for (int i = 0; i < s.length; i++) {
            ticks[i] = i;
        }
        Timeseries ts = new Timeseries(s, ticks);
        return ts2saxZnormByCuts(ts, windowSize, paaSize, cuts);
    }

    /**
     * Convert the timeseries into SAX string representation. It doesn't
     * normalize anything.
     *
     * @param ts The timeseries given.
     * @param windowSize The sliding window size used.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param cuts The alphabet cuts to use.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     */
    public static SAXFrequencyData ts2saxNoZnormByCuts(Timeseries ts, int windowSize, int paaSize,
            double[] cuts) throws TSException {

        // Initialize symbolic result data
        SAXFrequencyData res = new SAXFrequencyData();
        String previousString = "";

        // scan across the time series extract sub sequences, and converting
        // them to strings
        for (int i = 0; i < ts.size() - (windowSize - 1); i++) {

            // fix the current subsection
            Timeseries subSection = ts.subsection(i, i + windowSize - 1);

            // Z normalize it
            // subSection = TSUtils.normalize(subSection);
            // perform PAA conversion if needed
            Timeseries paa;
            try {
                paa = TSUtils.paa(subSection, paaSize);
            } catch (CloneNotSupportedException e) {
                throw new TSException("Unable to clone: " + StackTrace.toString(e));
            }

            // Convert the PAA to a string.
            char[] currentString = TSUtils.ts2StringWithNaNByCuts(paa, cuts);

            // check if previous one was the same, if so, ignore that (don't
            // know why though, but guess
            // cause we didn't advance much on the timeseries itself)
            if (!previousString.isEmpty() && previousString.equalsIgnoreCase(new String(currentString))) {
                previousString = new String(currentString);
                continue;
            }
            previousString = new String(currentString);
            res.put(new String(currentString), i);
        }
        return res;
    }

    /**
     * Convert the timeseries into SAX string representation.
     *
     * @param ts The timeseries given.
     * @param windowSize The sliding window size used.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param alphabet The alphabet to use.
     * @param alphabetSize The alphabet size used.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     * @throws CloneNotSupportedException
     */
    public static SAXFrequencyData ts2saxZNorm(Timeseries ts, int windowSize, int paaSize,
            Alphabet alphabet, int alphabetSize) throws TSException, CloneNotSupportedException {

        if (alphabetSize > alphabet.getMaxSize()) {
            throw new TSException("Unable to set the alphabet size greater than " + alphabet.getMaxSize());
        }

        return ts2saxZnormByCuts(ts, windowSize, paaSize, alphabet.getCuts(alphabetSize));

    }

    /**
     * Convert the timeseries into SAX string representation.
     *
     * @param ts The timeseries given.
     * @param windowSize The sliding window size used.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param alphabet The alphabet to use.
     * @param alphabetSize The alphabet size used.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     */
    public static SAXFrequencyData ts2saxNoZnorm(Timeseries ts, int windowSize, int paaSize,
            Alphabet alphabet, int alphabetSize) throws TSException {

        if (alphabetSize > alphabet.getMaxSize()) {
            throw new TSException("Unable to set the alphabet size greater than " + alphabet.getMaxSize());
        }

        return ts2saxNoZnormByCuts(ts, windowSize, paaSize, alphabet.getCuts(alphabetSize));

    }

    /**
     * Convert the timeseries into SAX string representation.
     *
     * @param ts The timeseries given.
     * @param paaSize The number of the points used in the PAA reduction of the
     * time series.
     * @param alphabet The alphabet to use.
     * @param alphabetSize The alphabet size used.
     * @return The SAX representation of the timeseries.
     * @throws TSException If error occurs.
     * @throws CloneNotSupportedException
     */
    public static String ts2string(Timeseries ts, int paaSize, Alphabet alphabet, int alphabetSize)
            throws TSException, CloneNotSupportedException {

        if (alphabetSize > alphabet.getMaxSize()) {
            throw new TSException("Unable to set the alphabet size greater than " + alphabet.getMaxSize());
        }

        int tsLength = ts.size();
        if (tsLength == paaSize) {
            return new String(TSUtils.ts2String(TSUtils.zNormalize(ts), alphabet, alphabetSize));
        } else {
            // perform PAA conversion
            Timeseries PAA;
            try {
                PAA = TSUtils.paa(TSUtils.zNormalize(ts), paaSize);
            } catch (CloneNotSupportedException e) {
                throw new TSException("Unable to clone: " + StackTrace.toString(e));
            }
            return new String(TSUtils.ts2String(PAA, alphabet, alphabetSize));
        }
    }

    /**
     * Compute the distance between the two strings, this function use the
     * numbers associated with ASCII codes, i.e. distance between a and b would
     * be 1.
     *
     * @param a The first string.
     * @param b The second string.
     * @return The pairwise distance.
     * @throws TSException if length are differ.
     */
    public static int strDistance(char[] a, char[] b) throws TSException {
        if (a.length == b.length) {
            int distance = 0;
            for (int i = 0; i < a.length; i++) {
                int tDist = Math.abs(Character.getNumericValue(a[i]) - Character.getNumericValue(b[i]));
                if (tDist > 1) {
                    distance += tDist;
                }
            }
            return distance;
        } else {
            throw new TSException("Unable to compute SAX distance, string lengths are not equal");
        }
    }

    /**
     * Compute the distance between the two chars based on the ASCII symbol
     * codes.
     *
     * @param a The first char.
     * @param b The second char.
     * @return The distance.
     */
    public static int strDistance(char a, char b) {
        return Math.abs(Character.getNumericValue(a) - Character.getNumericValue(b));
    }

    /**
     * This function implements SAX MINDIST function which uses alphabet based
     * distance matrix.
     *
     * @param a The SAX string.
     * @param b The SAX string.
     * @param distanceMatrix The distance matrix to use.
     * @return distance between strings.
     * @throws TSException If error occurs.
     */
    public static double saxMinDist(char[] a, char[] b, double[][] distanceMatrix) throws TSException {
        if (a.length == b.length) {
            double dist = 0.0D;
            for (int i = 0; i < a.length; i++) {
                if (Character.isLetter(a[i]) && Character.isLetter(b[i])) {
                    int numA = Character.getNumericValue(a[i]) - 10;
                    int numB = Character.getNumericValue(b[i]) - 10;
                    if (numA > 19 || numA < 0 || numB > 19 || numB < 0) {
                        throw new TSException("The character index greater than 19 or less than 0!");
                    }
                    double localDist = distanceMatrix[numA][numB];
                    dist += localDist;
                } else {
                    throw new TSException("Non-literal character found!");
                }
            }
            return dist;
        } else {
            throw new TSException("Data arrays lengths are not equal!");
        }
    }

    public MotifRecords series2Motifs(double[] series, int windowSize, int alphabetSize,
            int motifsNumToReport, SlidingWindowMarkerAlgorithm markerAlgorithm) throws TrieException,
            TSException {
        // init the SAX structures
        //
        SAXTrie trie = new SAXTrie(series.length - windowSize, alphabetSize);

        StringBuilder sb = new StringBuilder();
        sb.append("data size: ").append(series.length);

        double max = TSUtils.max(series);
        sb.append("; max: ").append(max);

        double min = TSUtils.min(series);
        sb.append("; min: ").append(min);

        double mean = TSUtils.mean(series);
        sb.append("; mean: ").append(mean);

        int nans = TSUtils.countNaN(series);
        sb.append("; NaNs: ").append(nans);

        consoleLogger.fine(sb.toString());
        consoleLogger.fine("window size: " + windowSize + ", alphabet size: " + alphabetSize
                + ", SAX Trie size: " + (series.length - windowSize));

        Alphabet normalA = new NormalAlphabet();

        Date start = new Date();
        // build the trie
        //
        int currPosition = 0;
        while ((currPosition + windowSize) < series.length) {
            // get the window SAX representation
            double[] subSeries = getSubSeries(series, currPosition, currPosition + windowSize);
            char[] saxVals = getSaxVals(subSeries, windowSize, normalA.getCuts(alphabetSize));
            // add result to the structure
            trie.put(String.valueOf(saxVals), currPosition);
            // increment the position
            currPosition++;
        }
        Date end = new Date();
        consoleLogger.fine("trie built in: " + timeToString(start.getTime(), end.getTime()));

        start = new Date();
        MotifRecords motifs = getMotifs(trie, motifsNumToReport);
        end = new Date();

        consoleLogger.fine("motifs retrieved in: " + timeToString(start.getTime(), end.getTime()));

        return motifs;
    }

    private static void cleanUpFrequencies(ArrayList<SAXTrieHitEntry> frequencies,
            String currentWord, int startPosition) {
        int i = startPosition + 1;
        while (i < frequencies.size()) {
            if (currentWord.equalsIgnoreCase(String.valueOf(frequencies.get(i).getStr()))) {
                frequencies.remove(i);
            } else {
                i++;
            }
        }

    }

    /**
     * Get N top motifs from trie.
     *
     * @param trie The trie.
     * @param maxMotifsNum The number of motifs to report.
     * @return The motifs collection.
     * @throws TrieException If error occurs.
     */
    private static MotifRecords getMotifs(SAXTrie trie, int maxMotifsNum) throws TrieException {

        MotifRecords res = new MotifRecords(maxMotifsNum);

        ArrayList<SAXTrieHitEntry> frequencies = trie.getFrequencies();

        Collections.sort(frequencies);

        // all sorted - from one end we have unique words - those discords
        // from the other end - we have motifs - the most frequent entries
        //
        // what I'll do here - is to populate non-trivial frequent entries into
        // the resulting container
        //
        // picking those non-trivial patterns this method job
        // non-trivial here means the one which are not the same letters
        //
        Set<SAXTrieHitEntry> seen = new TreeSet<SAXTrieHitEntry>();

        int counter = 0;
        // iterating backward - collection is sorted
        for (int i = frequencies.size() - 1; i >= 0; i--) {
            SAXTrieHitEntry entry = frequencies.get(i);
            if (entry.isTrivial(2) || seen.contains(entry) || (2 > entry.getFrequency())) {
                if ((2 > entry.getFrequency())) {
                    break;
                }
                continue;
            } else {
                counter += 1;
                res.add(new MotifRecord(entry.getStr(), trie.getOccurences(entry.getStr())));
                seen.add(entry);
                if (counter > maxMotifsNum) {
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Convert real-valued series into symbolic representation.
     *
     * @param vals Real valued timeseries.
     * @param windowSize The PAA window size.
     * @param cuts The cut values array used for SAX transform.
     * @return The symbolic representation of the given real time-series.
     * @throws TSException If error occurs.
     */
    public static char[] getSaxVals(double[] vals, int windowSize, double[] cuts) throws TSException {
        char[] saxVals;
        if (windowSize == cuts.length + 1) {
            saxVals = TSUtils.ts2String(TSUtils.zNormalize(vals), cuts);
        } else {
            saxVals = TSUtils.ts2String(TSUtils.zNormalize(TSUtils.paa(vals, cuts.length + 1)), cuts);
        }
        return saxVals;
    }

    /**
     * Extracts sub-series from the WEKA-style series.
     *
     * @param data The series.
     * @param attribute The data-bearing attribute.
     * @param start The start timestamp.
     * @param end The end timestamp
     * @return sub-series from start to end.
     */
    private static double[] getSubSeries(Instances data, Attribute attribute, int start, int end) {
        Instances subList = new Instances(data, start, end - start);
        double[] vals = new double[end - start];
        for (int i = 0; i < end - start; i++) {
            vals[i] = subList.instance(i).value(attribute.index());
        }
        return vals;
    }

    /**
     * Converts Instances into double array.
     *
     * @param tsData The instances data.
     * @param dataAttribute The attribute to use in conversion.
     * @return real-valued array.
     */
    public static double[] toRealSeries(Instances tsData, Attribute dataAttribute) {
        double[] vals = new double[tsData.numInstances()];
        for (int i = 0; i < tsData.numInstances(); i++) {
            vals[i] = tsData.instance(i).value(dataAttribute.index());
        }
        return vals;
    }

    /**
     * Extracts sub-series from series.
     *
     * @param data The series.
     * @param start The start position.
     * @param end The end position
     * @return sub-series from start to end.
     */
    public static double[] getSubSeries(double[] data, int start, int end) {
        double[] vals = new double[end - start];
        for (int i = 0; i < end - start; i++) {
            vals[i] = data[start + i];
        }
        return vals;
    }

    /**
     * Generic method to convert the milliseconds into the elapsed time string.
     *
     * @param start Start timestamp.
     * @param finish End timestamp.
     * @return String representation of the elapsed time.
     */
    public static String timeToString(long start, long finish) {
        long diff = finish - start;

        long secondInMillis = 1000;
        long minuteInMillis = secondInMillis * 60;
        long hourInMillis = minuteInMillis * 60;
        long dayInMillis = hourInMillis * 24;
        long yearInMillis = dayInMillis * 365;

        @SuppressWarnings("unused")
        long elapsedYears = diff / yearInMillis;
        diff = diff % yearInMillis;

        @SuppressWarnings("unused")
        long elapsedDays = diff / dayInMillis;
        diff = diff % dayInMillis;

        @SuppressWarnings("unused")
        long elapsedHours = diff / hourInMillis;
        diff = diff % hourInMillis;

        long elapsedMinutes = diff / minuteInMillis;
        diff = diff % minuteInMillis;

        long elapsedSeconds = diff / secondInMillis;
        diff = diff % secondInMillis;

        long elapsedMilliseconds = diff % secondInMillis;

        return elapsedMinutes + "m " + elapsedSeconds + "s " + elapsedMilliseconds + "ms";
    }

}
