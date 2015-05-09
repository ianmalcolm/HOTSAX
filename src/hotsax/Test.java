/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hotsax;

import Distance.ED;
import SAXFactory.DiscordRecords;
import SAXFactory.SAXFactory;
import java.util.Date;
import java.util.logging.Level;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

/**
 *
 * @author ian
 */
public class Test {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ParseException, Exception {
        // TODO code application logic here
        Date totalstart = new Date();

        int SLIDING_WINDOW_SIZE = 360;
        int ALPHABET_SIZE = 5;
        String DATA_VALUE_ATTRIBUTE = "value0";
        String FILE = "../datasets/ecg/ecg102.arff";
        int LENGTH = 16000;
        int REPORT_NUM = 5;
        Level level = Level.FINE;

        if (args.length > 0) {
            Options options = new Options();
            options.addOption("len", true, "Set the length of dataset");
            options.addOption("rep", true, "The number of reported discords");
            options.addOption("fil", true, "The file name of the dataset");
            options.addOption("att", true, "The abbribute of instances, see the introduction of arff in WEKA for details");
            options.addOption("alp", true, "The size of alphabets");
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

        System.out.println("-fil " + FILE + " -att " + DATA_VALUE_ATTRIBUTE + " -win " + SLIDING_WINDOW_SIZE + " -len " + LENGTH + " -rep " + REPORT_NUM + " -alp " + ALPHABET_SIZE);

        SAXFactory.setLoggerLevel(level);
        // get the data first
        Instances tsData = ConverterUtils.DataSource.read(FILE);
        Attribute dataAttribute = tsData.attribute(DATA_VALUE_ATTRIBUTE);
        double[] timeseries = SAXFactory.toRealSeries(tsData, dataAttribute);

        if (LENGTH
                > 0) {
            timeseries = SAXFactory.getSubSeries(timeseries, 0, LENGTH);
        }

        DiscordRecords discords = HOTSAX.findDiscords(timeseries, SLIDING_WINDOW_SIZE, ALPHABET_SIZE, REPORT_NUM);

        for (int i = 0; i < discords.getSize(); i++) {
            int start = discords.getKthPosition(i);
            int end = start + SLIDING_WINDOW_SIZE;
            double dist = discords.getKthDistance(i);
            System.out.println(start + "\t" + end + "\t" + dist);
        }

        Date totalend = new Date();
        System.out.println((totalend.getTime() - totalstart.getTime()) / 1000);
        System.out.println(ED.getCounter());
    }

}
