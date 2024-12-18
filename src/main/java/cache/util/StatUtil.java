package cache.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author Manjunath Kustagi
 */
public class StatUtil {

    public static final double M_PI = 3.14159265358979323846;
    public static final double M_SQRT2 = 1.41421356237309504880;
    public static final int NUM_BINS = 1000;

    public static double computeShannonEntropy(List<Double> values) {
        double entropy = 0d;
        SummaryStatistics summaryStats = new SummaryStatistics();
        values.stream().forEach((value) -> {
            summaryStats.addValue(value);
        });
        double mean = summaryStats.getMean();
        double sd = summaryStats.getStandardDeviation();
        double _max = summaryStats.getMax();
        double _min = summaryStats.getMin();
        if (sd > 0) {
            double binSize = (_max - _min) / 10000d;
            NormalDistribution normalDist = new NormalDistribution(mean, sd);
            for (Double v : values) {
                double probability = normalDist.probability(v, v + binSize);
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        return entropy;
    }

    public static double computeVariance(List<Double> values) {
        SummaryStatistics summaryStats = new SummaryStatistics();

        values.stream().forEach((Double sequence) -> {
            summaryStats.addValue(sequence);
        });

        return summaryStats.getVariance();
    }

    public static double median(List<Double> sortedData, int n) {
        double median;
        int lhs = (n - 1) / 2;
        int rhs = n / 2;

        if (n == 0) {
            return 0.0;
        }
        if (lhs == rhs) {
            median = sortedData.get(lhs);
        } else {
            median = (sortedData.get(lhs) + sortedData.get(rhs)) / 2.0;
        }

        return median;
    }

    public static double interQuartileRange(List<Double> sortedData, int size) {

        Collections.sort(sortedData);

        int medianIndex = (size + 2) / 2;

        List<Double> subset = new ArrayList<>();

        for (int i = 0; i < (medianIndex - 1); i++) {
            subset.add(sortedData.get(i));
        }

        double q1 = median(subset, subset.size());

        // Q(3) = median(y(find(y>median(y))));
        medianIndex = (size + 1) / 2;

        subset.clear();

        for (int i = medianIndex; i < size; i++) {
            subset.add(sortedData.get(i));
        }

        double q3 = median(subset, subset.size());

        subset.clear();

        return q3 - q1;
    }

    public static double normalPDF(double dx, double sigma) {
        double y = Math.exp(-0.5 * Math.pow((dx / sigma), 2)) / (Math.sqrt(2 * M_PI) * sigma);
        return y;
    }

    public static double multinormalPDF(double dx, double dy, double sigmaX, double sigmaY) {
        double y = Math.exp(-0.5 * (Math.pow((dx / sigmaX), 2) + Math.pow((dy / sigmaY), 2)));
        y = y / (2 * M_PI * sigmaX * sigmaY);
        return y;
    }

    public static int[] allRowBasedIndicesOf(double[][] objects, double o) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < objects.length; i++) {
            double o1 = objects[i][0];
            if (o1 == o) {
                indices.add(i);
            }
        }
        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }
        return indicesArray;
    }

    public static int[] allRowBasedIndicesOfComplement(double[][] objects, double o) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < objects.length; i++) {
            double o1 = objects[i][0];
            if (o1 != o) {
                indices.add(i);
            }
        }
        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }
        return indicesArray;
    }

    /**
     * Simple insertion sort.
     *
     * @param a an array of items.
     * @param c a comparator
     */
    public static void insertionSort(List a, Comparator c) {
        for (int p = 1; p < a.size(); p++) {
            Object tmp = a.get(p);
            int j = p;

            for (; j > 0 && c.compare(tmp, a.get(j - 1)) < 0; j--) {
                a.set(j, a.get(j - 1));
            }
            a.set(j, tmp);
        }
    }

    /**
     * Mergesort algorithm.
     *
     * @param a an array of Comparable items.
     */
    public static void mergeSort(Comparable[] a) {
        Comparable[] tmpArray = new Comparable[a.length];
        mergeSort(a, tmpArray, 0, a.length - 1);
    }

    /**
     * Internal method that makes recursive calls.
     *
     * @param a an array of Comparable items.
     * @param tmpArray an array to place the merged result.
     * @param left the left-most index of the subarray.
     * @param right the right-most index of the subarray.
     */
    private static void mergeSort(Comparable[] a, Comparable[] tmpArray,
            int left, int right) {
        if (left < right) {
            int center = (left + right) / 2;
            mergeSort(a, tmpArray, left, center);
            mergeSort(a, tmpArray, center + 1, right);
            merge(a, tmpArray, left, center + 1, right);
        }
    }

    /**
     * Internal method that merges two sorted halves of a subarray.
     *
     * @param a an array of Comparable items.
     * @param tmpArray an array to place the merged result.
     * @param leftPos the left-most index of the subarray.
     * @param rightPos the index of the start of the second half.
     * @param rightEnd the right-most index of the subarray.
     */
    private static void merge(Comparable[] a, Comparable[] tmpArray,
            int leftPos, int rightPos, int rightEnd) {
        int leftEnd = rightPos - 1;
        int tmpPos = leftPos;
        int numElements = rightEnd - leftPos + 1;

        // Main loop
        while (leftPos <= leftEnd && rightPos <= rightEnd) {
            if (a[leftPos].compareTo(a[rightPos]) <= 0) {
                tmpArray[tmpPos++] = a[leftPos++];
            } else {
                tmpArray[tmpPos++] = a[rightPos++];
            }
        }

        while (leftPos <= leftEnd) // Copy rest of first half
        {
            tmpArray[tmpPos++] = a[leftPos++];
        }

        while (rightPos <= rightEnd) // Copy rest of right half
        {
            tmpArray[tmpPos++] = a[rightPos++];
        }

        // Copy tmpArray back
        for (int i = 0; i < numElements; i++, rightEnd--) {
            a[rightEnd] = tmpArray[rightEnd];
        }
    }

    public static class Gene {

        double x;
        int xi; // index of x
        int maId;

        public Gene(int mId, double value) {
            maId = mId;
            x = value;
        }

        @Override
        public boolean equals(Object o) {
            Gene gp = (Gene) o;
            return (gp.maId == maId);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.maId;
            return hash;
        }

        @Override
        public String toString() {
            return "x:" + x + "xi:" + x + "maId:" + maId;
        }
    }

    public static class Sort_ID implements Comparator<Gene> {

        @Override
        public int compare(Gene a, Gene b) {
            return (new Integer(a.maId)).compareTo(b.maId);
        }
    }

    public static class Sort_Gene implements Comparator<Gene> {

        @Override
        public int compare(Gene a, Gene b) {
            if (a.x != b.x) {
                if (a.x < b.x) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return (int) Math.signum(a.maId - b.maId);
            }
        }
    }

    public static class Gene_Pair {

        double x;
        double y;
        int xi; // index of x
        int yi; // index y
        int maId;

        @Override
        public boolean equals(Object o) {
            Gene_Pair gp = (Gene_Pair) o;
            return (gp.x == x && gp.y == y && gp.xi == xi && gp.yi == y);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
            hash = 53 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
            hash = 53 * hash + this.xi;
            hash = 53 * hash + this.yi;
            return hash;
        }
    }

    public static class Sort_X implements Comparator<Gene_Pair> {

        @Override
        public int compare(Gene_Pair a, Gene_Pair b) {
            if (a.x != b.x) {
                if (a.x < b.x) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return (int) Math.signum(a.maId - b.maId);
            }
        }
    }

    public static class Sort_Y implements Comparator<Gene_Pair> {

        @Override
        public int compare(Gene_Pair a, Gene_Pair b) {
            if (a.y != b.y) {
                if (a.y < b.y) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return (int) Math.signum(a.maId - b.maId);
            }
        }
    }

    public static class Shuffle {

        static java.util.Random rand = new java.util.Random(System.currentTimeMillis());
        // construct a random permutation of the ints 0 .. n - 1
        // represented as an int array of length n
        //

        public static int[] permutation(int n) {

            assert n > 0;
            //intitial element order is irrelevant so long as each int 1..n occurs exactly once
            //inorder initialization assures that is the case

            int[] sample = new int[n];
            for (int k = 0; k < sample.length; k++) {
                sample[k] = k;
            }
            //loop invariant: the tail of the sample array is randomized.
            //Intitally the tail is empty; at each step move a random
            //element from front of array into the tail, then decrement boundary of tail
            int last = sample.length - 1;   //last is maximal index of elements not in the tail

            while (last > 0) {
                // Select random index in range 0..last, and swap its contents with those at last
                // The null swap is allowed; it should be possible that sample[k] does not change
                swap(rand.nextInt(last + 1), last, sample);
                last -= 1;
            }
            return sample;
        }

        // swap the elements at indices j and k
        // j and k need not be distinct, allowing for null swaps
        //
        private static void swap(int j, int k, int[] array) {
            int temp = array[k];
            array[k] = array[j];
            array[j] = temp;
        }

        public static int[] sampleWithoutReplacement(int N, int M) {

            // create permutation 0, 1, ..., N-1
            int[] perm = new int[N];
            for (int i = 0; i < N; i++) {
                perm[i] = i;            // create random sample in perm[0], perm[1], ..., perm[M-1]
            }
            for (int i = 0; i < M; i++) {

                // random integer between i and N - 1
                int r = i + (int) (rand.nextInt(N - i));

                // swap elements at indices i and r
                int t = perm[r];
                perm[r] = perm[i];
                perm[i] = t;
            }

            int[] subset = new int[M];
            System.arraycopy(perm, 0, subset, 0, M);
            return subset;
        }

        public static int[] sampleWithReplacement(int N, int M) {

            // create permutation 0, 1, ..., N-1
            int[] perm = new int[N];
            for (int i = 0; i < N; i++) {
                perm[i] = i;            // create random sample in perm[0], perm[1], ..., perm[M-1]
            }
            int[] subset = new int[M];
            for (int i = 0; i < M; i++) {

                // random integer between 0 and N - 1
                int r = (int) (rand.nextInt(N - 1));

                //sample from N
                subset[i] = perm[r];
            }
            return subset;
        }
    }
}
