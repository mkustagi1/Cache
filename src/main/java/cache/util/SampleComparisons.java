package cache.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Manjunath Kustagi
 */
public class SampleComparisons {

    public static void main(String[] args) {
        String baseDir = "/cache_data/mappedReads";
        List<Long> experiments = findProcessedExperiments(baseDir);
        Collections.sort(experiments);

        Map<Long, Long> counts = new HashMap<>();
        experiments.stream().forEach((experimentId) -> {
            Long count = readCount(baseDir, experimentId);
            counts.put(experimentId, count);
        });

        Map<Long, List<Record>> data = new HashMap<>();
        experiments.stream().forEach((experimentId) -> {
            List<Record> row = readData(baseDir, experimentId, (counts.get(experimentId).doubleValue() / 1000000d));
            data.put(experimentId, row);
        });

        for (int i = 0; i < experiments.size() - 1; i++) {
            String newDir = baseDir + File.separator + "comparisons" + File.separator + experiments.get(i);
            boolean success = (new File(newDir)).mkdirs();
            if (!success) {
                (new File(newDir)).mkdir();
            }
            for (int j = (i + 1); j < experiments.size(); j++) {
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(newDir + File.separator + "comparison_" + experiments.get(i) + "_" + experiments.get(j) + ".txt"));
                    List<Record> a = data.get(experiments.get(i));
                    List<Record> b = data.get(experiments.get(j));
                    List<Comparison> comparisonsAB = computeComparison(a, b);
                    Collections.sort(comparisonsAB);
                    
                    for (int k = 0; k < 2000; k++) {
                        Comparison c1 = comparisonsAB.get(k);
                        String line = c1.gene + "\t" + c1.transcriptA + "\t" + c1.transcriptB + "\t" + c1.diffAB + "\t" + c1.diffRpkm; 
                        bw.write(line);
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();
                    
                    Collections.sort(comparisonsAB, (Comparison t, Comparison t1) -> t.diffAB.compareTo(t1.diffAB));
                    bw = new BufferedWriter(new FileWriter(newDir + File.separator + "comparison_" +  experiments.get(j) + "_" + experiments.get(i)+ ".txt"));
                    for (int k = 0; k < 2000; k++) {
                        Comparison c1 = comparisonsAB.get(k);
                        String line = c1.gene + "\t" + c1.transcriptA + "\t" + c1.transcriptB + "\t" + (-1 * c1.diffAB) + "\t" + (-1 * c1.diffRpkm); 
                        bw.write(line);
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();

                } catch (IOException ex) {
                    Logger.getLogger(SampleComparisons.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static List<Comparison> computeComparison(List<Record> a, List<Record> b) {
        List<Comparison> comparisons = new ArrayList<>();
        a.stream().forEach((recordA) -> {
            int index = Collections.binarySearch(b, recordA);
            if (index >= 0) {
                Record recordB = b.get(index);
                Comparison c = new Comparison();
                c.gene = recordA.gene;
                c.transcriptA = recordA.transcript;
                c.transcriptB = recordB.transcript;
                c.diffAB = recordA.referenceReadCount - recordB.referenceReadCount;
                c.diffRpkm = recordA.rpkm - recordB.rpkm; 
                comparisons.add(c);
            }
        });
        return comparisons;
    }

    private static Long readCount(String baseDir, Long experimentId) {
        BufferedReader br = null;
        Long count = 0l;
        try {
            File file = new File(baseDir + File.separator + "totalCount_" + experimentId + ".txt");
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            count = Long.parseLong(line.trim());
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(SampleComparisons.class.getName()).log(Level.SEVERE, null, ex);
        }
        return count;
    }

    private static List<Record> readData(String baseDir, Long experimentId, Double count) {
        BufferedReader br = null;
        List<Record> row = new ArrayList<>();
        try {
            File file = new File(baseDir + File.separator + "MappedTranscriptsMostExpressed_" + experimentId + ".txt");
            br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\t");
                Record record = new Record();
                record.gene = tokens[0].trim();
                record.referenceReadCount = Long.parseLong(tokens[1].trim());
                record.tid = Long.parseLong(tokens[2].trim());
                record.transcript = tokens[3].trim();
                record.transcriptLength = Integer.parseInt(tokens[4].trim());
                record.isoformNumber = Integer.parseInt(tokens[5].trim());
                record.otherReadCount = Long.parseLong(tokens[6].trim());
                record.rpkm = ((record.referenceReadCount.doubleValue() / (record.transcriptLength.doubleValue() / 1000d)) / count);
                row.add(record);
            }
            Collections.sort(row);
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(SampleComparisons.class.getName()).log(Level.SEVERE, null, ex);
        }
        return row;
    }

    private static List<Long> findProcessedExperiments(String baseDir) {
        File directory = new File(baseDir);
        String[] files = directory.list();
        Arrays.sort(files);
        List<Long> experiments = new ArrayList<>();
        for (String file : files) {
            if (file.startsWith("MappedTranscriptsMostExpressed")) {
                String[] tokens = file.split("_");
                String[] tks = tokens[1].split("\\.");
                Long experimentId = Long.parseLong(tks[0]);
                experiments.add(experimentId);
            }
        }
        return experiments;
    }

    static class Comparison implements Comparable<Comparison> {

        String gene;
        String transcriptA;
        String transcriptB;
        Long diffAB;
        Double diffRpkm;
        
        @Override
        public int compareTo(Comparison t) {
            return t.diffAB.compareTo(diffAB);
        }
    }

    static class Record implements Comparable<Record> {

        String gene;
        String transcript;
        Long tid;
        Long referenceReadCount;
        Integer transcriptLength;
        Integer isoformNumber;
        Long otherReadCount;
        Double rpkm;

        @Override
        public int compareTo(Record t) {
            return gene.compareTo(t.gene);
        }

        @Override
        public boolean equals(Object o) {
            return gene.equals(((Record) o).gene);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }
    }
}
