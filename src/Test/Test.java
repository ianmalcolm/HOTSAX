/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Test;

import SAXFactory.DiscordRecords;
import SAXFactory.SAXFactory;
import SAXFactory.TSUtils;
import hotsax.DataHandler;
import hotsax.Distance;
import hotsax.HOTSAX;
import java.util.BitSet;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 *
 * @author ian
 */
public class Test {

    static {
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(ch);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ParseException, Exception {
        // TODO code application logic here

        int SLIDING_WINDOW_SIZE = 100;
        int ALPHABET_SIZE = 5;
        int DIMENSION = 5;
        String DATA_VALUE_ATTRIBUTE = "value0";
        String FILE = "../datasets/exnoise/exnoise.arff";
        int LENGTH = -1;
        int REPORT_NUM = 1;
        Level level = Level.FINE;
        int OFFSET = 0;
        double[] series;

        Date totalstart = new Date();

        if (args.length > 0) {
            Options options = new Options();
            options.addOption("len", true, "Set the length of dataset, -1 for using the entire dataset, default is -1");
            options.addOption("ofs", true, "Set the offset of the dataset, defalut is 0");
            options.addOption("rep", true, "The number of reported discords");
            options.addOption("fil", true, "The file name of the dataset");
            options.addOption("att", true, "The abbribute of instances, see the introduction of arff in WEKA for details");
            options.addOption("alp", true, "The size of alphabets, typical value 5");
            options.addOption("dim", true, "The size of dimension, typical value 5");
            options.addOption("win", true, "The size of sliding window");
            options.addOption("log", true, "The log level");
            options.addOption("h", false, "Print help message");

            CommandLineParser parser = new BasicParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("HOTSAX", options);
                return;
            }

            if (cmd.hasOption("alp")) {
                ALPHABET_SIZE = Integer.parseInt(cmd.getOptionValue("alp"));
            }
            if (cmd.hasOption("dim")) {
                DIMENSION = Integer.parseInt(cmd.getOptionValue("dim"));
            }
            if (cmd.hasOption("ofs")) {
                OFFSET = Integer.parseInt(cmd.getOptionValue("ofs"));
            }
            if (cmd.hasOption("win")) {
                SLIDING_WINDOW_SIZE = Integer.parseInt(cmd.getOptionValue("win"));
            }
            if (cmd.hasOption("len")) {
                LENGTH = Integer.parseInt(cmd.getOptionValue("len"));
            }
            if (cmd.hasOption("rep")) {
                REPORT_NUM = Integer.parseInt(cmd.getOptionValue("rep"));
            }
            if (cmd.hasOption("fil")) {
                FILE = cmd.getOptionValue("fil");
            }
            if (cmd.hasOption("att")) {
                DATA_VALUE_ATTRIBUTE = cmd.getOptionValue("att");
            }

            if (cmd.hasOption("log")) {
                if (cmd.getOptionValue("log").equalsIgnoreCase("OFF")) {
                    level = Level.OFF;
                } else if (cmd.getOptionValue("log").equalsIgnoreCase("SEVERE")) {
                    level = Level.SEVERE;
                } else if (cmd.getOptionValue("log").equalsIgnoreCase("WARNING")) {
                    level = Level.WARNING;
                } else if (cmd.getOptionValue("log").equalsIgnoreCase("INFO")) {
                    level = Level.INFO;
                } else if (cmd.getOptionValue("log").equalsIgnoreCase("CONFIG")) {
                    level = Level.CONFIG;
                } else if (cmd.getOptionValue("log").equalsIgnoreCase("FINE")) {
                    level = Level.FINE;
                } else if (cmd.getOptionValue("log").equalsIgnoreCase("FINER")) {
                    level = Level.FINER;
                } else if (cmd.getOptionValue("log").equalsIgnoreCase("FINEST")) {
                    level = Level.FINEST;
                }
            }
        }

        System.out.println("-fil " + FILE
                + " -att " + DATA_VALUE_ATTRIBUTE
                + " -win " + SLIDING_WINDOW_SIZE
                + " -dim " + DIMENSION
                + " -ofs " + OFFSET
                + " -len " + LENGTH
                + " -rep " + REPORT_NUM
                + " -alp " + ALPHABET_SIZE);

        HOTSAX.setLoggerLevel(level);
        // get the data first

        {
            String patternArff = "([^\\s]+(\\.(?i)(arff))$)";
            Pattern pattern = Pattern.compile(patternArff);
            if (pattern.matcher(FILE).matches()) {
                Instances tsData = ConverterUtils.DataSource.read(FILE);
                Attribute dataAttribute = tsData.attribute(DATA_VALUE_ATTRIBUTE);
                series = SAXFactory.toRealSeries(tsData, dataAttribute);
                if (LENGTH > 0) {
                    series = SAXFactory.getSubSeries(series, OFFSET, OFFSET + LENGTH);
                }
            } else {
                series = new double[LENGTH];
                int i = 0;
                int ofsCnt = 1;
                try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
                    String line;
                    while (ofsCnt++ < OFFSET) {
                        line = br.readLine();
                        if (line == null) {
                            System.out.println("Not enough lines in " + FILE);
                            return;
                        }
                    }
                    while (i < LENGTH) {
                        line = br.readLine();
                        if (line == null) {
                            System.out.println("Not enough lines in " + FILE);
                            return;
                        }
                        series[i++] = Double.parseDouble(line);
                    }
                    br.close();
                }
            }
        }

        DataInMemory dh = new DataInMemory(series, SLIDING_WINDOW_SIZE);
        ED ed = new ED();
        HOTSAX hotsax = new HOTSAX(SLIDING_WINDOW_SIZE, ALPHABET_SIZE, DIMENSION, dh, ed);

        DiscordRecords discords = hotsax.findDiscords(REPORT_NUM);

        System.out.println("Find discords:\n" + discords.toString() + "\n");
        Date totalend = new Date();

        System.out.println("Discovery time elapsed: " + (totalend.getTime() - totalstart.getTime()) / 1000);
        System.out.println("Total count of the calls to the distance function: " + hotsax.totalcnt);
    }

}

class DataInMemory extends DataHandler {

    private double[] series;
    private int windowSize;
//    private double mean;
//    private double std;

    public DataInMemory(double[] _series, int _windowSize) {
        series = _series;
        windowSize = _windowSize;
//        mean = TSUtils.mean(series);
//        std = TSUtils.stDev(series);
    }

    @Override
    public long size() {
        return series.length - windowSize + 1;
    }

    @Override
    public double[] get(long i) {
        double[] subSeries = TSUtils.getSubSeries(series, (int) i, (int) i + windowSize);
        double mean = TSUtils.mean(subSeries);
        double std = TSUtils.stDev(subSeries);
        return TSUtils.zNormalize(subSeries, mean, std);
    }

    @Override
    public void mark(BitSet list, int id) {

        int pbegin = (int) (id - windowSize + 1);
        int pend = (int) (id + windowSize - 1);
        if (pbegin < 0) {
            pbegin = 0;
        }
        if (pend >= list.size()) {
            pend = list.size();
        }
        list.set(pbegin, pend);

    }

}

/**
 * @author Pavel Senin
 */
class ED extends Distance {

    private long cnt = 0;

    /**
     * Calculates the square of the Euclidean distance between two 1D points
     * represented by real values.
     *
     * @param p1 The first point.
     * @param p2 The second point.
     * @return The Square of Euclidean distance.
     */
    public static double distance2(double p1, double p2) {
        double temp = p1 - p2;
        return temp * temp;
    }

    /**
     * Calculates the square of the Euclidean distance between two
     * multidimensional points represented by the real vectors.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws TSException In the case of error.
     */
    public static double distance2(double[] point1, double[] point2) {
        assert point1.length == point2.length : "Exception in Euclidean distance: array lengths are not equal";
        Double sum = 0D;
        for (int i = 0; i < point1.length; i++) {
            double temp = point2[i] - point1[i];
            sum = sum + temp * temp;
        }
        return sum;
    }

    /**
     * Calculates the square of the Euclidean distance between two
     * multidimensional points represented by integer vectors.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws TSException In the case of error.
     */
    public static double distance2(int[] point1, int[] point2) {
        assert point1.length == point2.length : "Exception in Euclidean distance: array lengths are not equal";
        Double sum = 0D;
        for (int i = 0; i < point1.length; i++) {
            double temp = Integer.valueOf(point2[i]).doubleValue() - Integer.valueOf(point1[i]).doubleValue();
            sum = sum + temp * temp;
        }
        return sum;
    }

    /**
     * Calculates the Euclidean distance between two points.
     *
     * @param p1 The first point.
     * @param p2 The second point.
     * @return The Euclidean distance.
     */
    public static double distance(double p1, double p2) {
        double temp = (p1 - p2);
        double d = temp * temp;
        return Math.sqrt(d);
    }

    /**
     * Calculates the Euclidean distance between two points.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws TSException In the case of error.
     */
    @Override
    public double distance(double[] point1, double[] point2) {
        cnt++;
        return Math.sqrt(distance2(point1, point2));
    }

    /**
     * Calculates the Euclidean distance between two points.
     *
     * @param point1 The first point.
     * @param point2 The second point.
     * @return The Euclidean distance.
     * @throws TSException In the case of error.
     */
    public static double distance(int[] point1, int[] point2) {
        return Math.sqrt(distance2(point1, point2));
    }

    /**
     * Calculates euclidean distance between two one-dimensional time-series of
     * equal length.
     *
     * @param series1 The first series.
     * @param series2 The second series.
     * @return The eclidean distance.
     * @throws TSException if error occures.
     */
    public static double seriesDistance(double[] series1, double[] series2) {
        assert series1.length == series2.length : "Exception in Euclidean distance: array lengths are not equal";
        Double res = 0D;
        for (int i = 0; i < series1.length; i++) {
            res = res + distance2(series1[i], series2[i]);
        }
        return Math.sqrt(res);
    }

    /**
     * Calculates euclidean distance between two multi-dimensional time-series
     * of equal length.
     *
     * @param series1 The first series.
     * @param series2 The second series.
     * @return The eclidean distance.
     * @throws TSException if error occures.
     */
    public static double seriesDistance(double[][] series1, double[][] series2) {
        assert series1.length == series2.length : "Exception in Euclidean distance: array lengths are not equal";
        Double res = 0D;
        for (int i = 0; i < series1.length; i++) {
            res = res + distance2(series1[i], series2[i]);
        }
        return Math.sqrt(res);

    }

    @Override
    public void clearCount() {
        cnt = 0;
    }

    @Override
    public long getCount() {
        return cnt;
    }
}
