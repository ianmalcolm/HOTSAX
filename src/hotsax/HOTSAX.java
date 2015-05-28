/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hotsax;

import SAXFactory.DiscordRecords;
import SAXFactory.SAXFactory;
import SAXFactory.TSUtils;
import edu.hawaii.jmotif.sax.LargeWindowAlgorithm;
import edu.hawaii.jmotif.sax.SlidingWindowMarkerAlgorithm;
import edu.hawaii.jmotif.sax.alphabet.NormalAlphabet;
import edu.hawaii.jmotif.sax.datastructures.DiscordRecord;
import edu.hawaii.jmotif.sax.trie.SAXTrie;
import edu.hawaii.jmotif.sax.trie.SAXTrieHitEntry;
import edu.hawaii.jmotif.sax.trie.TrieException;
import edu.hawaii.jmotif.sax.trie.VisitRegistry;
import edu.hawaii.jmotif.timeseries.TSException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ian
 */
public class HOTSAX {

    private static final Logger logger = Logger.getLogger(HOTSAX.class.getName());
    public long totalcnt = 0;
    private int windowSize;
    private int alphabetSize;
    private SAXTrie trie;
    private int dimension;
    private DataHandler dh;
    private Distance df;
    private double[] NNDists;
    private ArrayList<SAXTrieHitEntry> frequencies;
    private ArrayList<Integer> allSubsequences;

    public HOTSAX(int ws, int as, int dim, DataHandler _dh, Distance _df) throws TrieException, TSException {

        windowSize = ws;
        alphabetSize = as;
        dimension = dim;
        dh = _dh;
        df = _df;

        NormalAlphabet normalA = new NormalAlphabet();
        trie = new SAXTrie((int) dh.size(), alphabetSize);

        // build the trie
        for (int i = 0; i < dh.size(); i++) {
            // get the window SAX representation
            char[] saxVals = SAXFactory.getSaxVals(dh.get(i), windowSize, normalA.getCuts(alphabetSize));
            // add result to the structure
            trie.put(String.valueOf(saxVals), i);
            // increment the position            
        }

        NNDists = new double[(int) dh.size()];
        for (int i = 0; i < NNDists.length; i++) {
            NNDists[i] = Double.MAX_VALUE;
        }

        frequencies = trie.getFrequencies();
        Collections.sort(frequencies);

        allSubsequences = getAllSubsequences();
    }

    public ArrayList<Integer> getAllSubsequences() {
        ArrayList<Integer> sslist = new ArrayList();
        for (SAXTrieHitEntry a : frequencies) {
            sslist.add(a.getPosition());
        }
        return sslist;
    }

    public static void setLoggerLevel(Level level) {
        logger.setLevel(level);
    }

    public DiscordRecords findDiscords(int reportNum) throws TSException, TrieException {

        BitSet visitedp = new BitSet((int) dh.size());

        logger.fine("starting discords finding routines");
        DiscordRecords drs = new DiscordRecords();

        while (drs.getSize() < reportNum) {
            logger.fine("Currently known discords: " + drs.getSize() + " out of " + reportNum);

            Date start = new Date();
            DiscordRecord bestDiscord = findNextDiscord(visitedp);
            Date end = new Date();

            // if the discord is null we getting out of the search            
            if (bestDiscord.getDistance() < 0 || bestDiscord.getPosition() < 0) {
                logger.fine("breaking the outer search loop, discords found: " + drs.getSize()
                        + " last seen discord: " + bestDiscord.toString());
                return drs;
            }

            // collect the result
            //
            drs.add(bestDiscord);

            logger.fine("Find #" + drs.getSize()
                    + " discord: " + bestDiscord.getPayload()
                    + " at " + bestDiscord.getPosition()
                    + ", distance " + bestDiscord.getDistance()
                    + ", elapsed time: " + SAXFactory.timeToString(start.getTime(), end.getTime())
                    + ", #distfunc: " + df.getCount());
            totalcnt += df.getCount();
            df.clearCount();

            // and maintain data structures
            dh.mark(visitedp, bestDiscord.getPosition());

        }
        return drs;
    }

    public DiscordRecord findNextDiscord(BitSet visitedp) throws TSException, TrieException {

        double bestSoFarDistance = Double.NEGATIVE_INFINITY;
        int bestSoFarPosition = Integer.MIN_VALUE;
        String bestSoFarString = "";

        //outer loop
        for (SAXTrieHitEntry outerOccurences : frequencies) {
            int p = outerOccurences.getPosition();
            if (visitedp.get(p)) {
                continue;
            }
            String outerWord = new String(outerOccurences.getStr());
            logger.finer("Position: " + p + "\tload: " + outerWord + "\tfrequency: " + outerOccurences.getFrequency());
            BitSet visitedq = new BitSet((int) dh.size());

            // inner loop
            boolean completeSearch = true;
            List<Integer> innerOccurences = trie.getOccurences(outerWord.toCharArray());
            for (int q : innerOccurences) {
                if (Math.abs(p - q) < windowSize) {
                    continue;
                }
                visitedq.set(q);

                double dist = df.distance(dh.get(p), dh.get(q));
                if (dist < NNDists[p]) {
                    NNDists[p] = dist;
                }
                if (dist < NNDists[q]) {
                    NNDists[q] = dist;
                }

                if (NNDists[p] < bestSoFarDistance) {
                    completeSearch = false;
                    break;
                }
            }

            if (completeSearch) {
                Collections.shuffle(allSubsequences);
                for (int q : allSubsequences) {
                    if (Math.abs(p - q) < windowSize) {
                        continue;
                    }
                    if (visitedq.get(q)) {
                        continue;
                    }
                    double dist = df.distance(dh.get(p), dh.get(q));
                    if (dist < NNDists[p]) {
                        NNDists[p] = dist;
                    }
                    if (dist < NNDists[q]) {
                        NNDists[q] = dist;
                    }
                    if (NNDists[p] < bestSoFarDistance) {
                        break;
                    }
                }
            }

            if (NNDists[p] > bestSoFarDistance) {
                bestSoFarDistance = NNDists[p];
                bestSoFarPosition = p;
                bestSoFarString = new String(outerWord);
                logger.fine("update best so far position: " + bestSoFarPosition + "\tdistance: " + bestSoFarDistance);

            }

        }

        return new DiscordRecord(bestSoFarPosition, bestSoFarDistance, bestSoFarString);
    }

}
