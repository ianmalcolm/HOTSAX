package SAXFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import edu.hawaii.jmotif.sax.datastructures.DiscordRecord;

/**
 * The discord records collection.
 *
 * @author Pavel Senin
 *
 */
public class DiscordRecords implements Iterable<DiscordRecord> {

    /**
     * Storage container.
     */
    private final LinkedList<DiscordRecord> discords;

    /**
     * Constructor.
     */

    public DiscordRecords() {
        discords = new LinkedList<DiscordRecord>();
    }

    // i starts from 0
//    public DiscordRecord get(int i) {
//        Collections.sort(discords);
//        return discords.get(i);
//    }
    /**
     * Add a new discord to the list. Here is a trick. This method will also
     * check if the current distance is less than best so far (best in the
     * table). If so - there is no need to continue that inner loop - the MAGIC
     * optimization.
     *
     * @param discord The discord instance to add.
     * @return if the discord got added.
     */
    public void add(DiscordRecord discord) {

            discords.add(discord);
            Collections.sort(discords);
    }

    /**
     * Returns the number of the top hits.
     *
     * @param num The number of instances to return. If the number larger than
     * the storage size - returns the storage as is.
     * @return the top discord hits.
     */
    public List<DiscordRecord> getTopHits(Integer num) {
        Collections.sort(discords);
        if (num >= this.discords.size()) {
            return this.discords;
        }
        List<DiscordRecord> res = this.discords.subList(this.discords.size() - num,
                this.discords.size());
        Collections.reverse(res);
        return res;
    }

    /**
     * Get the minimal distance found among all instances in the collection.
     *
     * @return The minimal distance found among all instances in the collection.
     */
//    public double getMinDistance() {
//        if (this.discords.size() > 0) {
//            Collections.sort(discords);
//            return discords.get(0).getDistance();
//        }
//        return -1;
//    }
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(1024);
        for (DiscordRecord r : discords) {
            sb.append("discord \"" + r.getPayload() + "\", at " + r.getPosition()
                    + " distance to closest neighbor: " + r.getDistance() + "\n");
        }
        return sb.toString();
    }

    @Override
    public Iterator<DiscordRecord> iterator() {
        return this.discords.iterator();
    }

    public int getSize() {
        return this.discords.size();
    }

    public double getWorstDistance() {
        if (this.discords.isEmpty()) {
            return 0D;
        }
        Collections.sort(discords);
        return discords.get(0).getDistance();
    }

    public String toCoordinates() {
        StringBuffer sb = new StringBuffer();
        for (DiscordRecord r : discords) {
            sb.append(r.getPosition() + ",");
        }
        return sb.delete(sb.length() - 1, sb.length()).toString();
    }

    public String toPayloads() {
        StringBuffer sb = new StringBuffer();
        for (DiscordRecord r : discords) {
            sb.append("\"" + r.getPayload() + "\",");
        }
        return sb.delete(sb.length() - 1, sb.length()).toString();
    }

    public String toDistances() {
        NumberFormat nf = new DecimalFormat("##0.####");
        StringBuffer sb = new StringBuffer();
        for (DiscordRecord r : discords) {
            sb.append(nf.format(r.getDistance()) + ",");
        }
        return sb.delete(sb.length() - 1, sb.length()).toString();
    }
}
