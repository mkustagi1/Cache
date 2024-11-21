package cache.workers;

import jaligner.Alignment;
import jaligner.Sequence;
import jaligner.SmithWatermanGotoh;
import jaligner.matrix.Matrix;
import jaligner.matrix.MatrixLoader;
import jaligner.matrix.MatrixLoaderException;
import jaligner.util.SequenceParser;
import jaligner.util.SequenceParserException;

/**
 *
 * @author Manjunath Kustagi
 */
public class CutAdapt {

    protected static Sequence threePrimeAdapter;
    protected static Sequence fivePrimeAdapter;
    protected static Sequence nineteenthNTMarker;
    protected static Sequence twentyFourthNTMarker;
    protected static Matrix identity;

    static {
        try {
            threePrimeAdapter = SequenceParser.parse("TCGTATGCCGTCTTCTGCTTG");
            fivePrimeAdapter = SequenceParser.parse("AATGATACGGCGACCACCGACAGGTTCAGAGTTCTACAGTCCGACGATC");
            nineteenthNTMarker = SequenceParser.parse("CGTACGCGGGTTTAAACGA");
            twentyFourthNTMarker = SequenceParser.parse("CGTACGCGGAATAGTTTAAACTGT");
            identity = MatrixLoader.load("IDENTITY");
        } catch (SequenceParserException | MatrixLoaderException spe) {
            spe.printStackTrace();
        }
    }

    public String cutAdapt(String s) {
        String trimmed = s;
        try {
            Sequence seq = SequenceParser.parse(trimmed);
            Alignment alignmentFivePrime = SmithWatermanGotoh.align(seq, fivePrimeAdapter, identity, 10f, 0.5f);
            if (alignmentFivePrime.checkScore()) {
                if (alignmentFivePrime.getScore() > 3f) {
                    int[] starts = new int[]{alignmentFivePrime.getStart1() + 1, alignmentFivePrime.getStart2() + 1};
                    int[] ends = getAlignmentEnds(alignmentFivePrime);
                    if (((ends[1] - starts[1] + 1) == fivePrimeAdapter.length()) || (starts[0] == 1 && ends[1] == fivePrimeAdapter.length())) {
                        int pos = ends[0];
                        if (pos > 0) {
//                            System.out.println ( new Pair().format(alignmentFivePrime) );
                            trimmed = trimmed.substring(pos, seq.length() - 1);
                        }                        
                    }
                }
            }

            seq = SequenceParser.parse(trimmed);
            Alignment alignmentThreePrime = SmithWatermanGotoh.align(seq, threePrimeAdapter, identity, 10f, 0.5f);
            if (alignmentThreePrime.checkScore()) {
                if (alignmentThreePrime.getScore() > 3f) {
                    int[] starts = new int[]{alignmentThreePrime.getStart1() + 1, alignmentThreePrime.getStart2() + 1};
                    int[] ends = getAlignmentEnds(alignmentThreePrime);
                    if (((ends[1] - starts[1] + 1) == threePrimeAdapter.length()) || (starts[1] == 1 && ends[0] == seq.length())) {
                        int pos = alignmentThreePrime.getStart1();
                        if (pos > 0) {
//                            System.out.println ( new Pair().format(alignmentThreePrime) );
                            trimmed = trimmed.substring(0, pos);
                        }                        
                    }
                }
            }

        } catch (SequenceParserException spe) {
            spe.printStackTrace();
        }
        return trimmed;
    }

    protected int[] getAlignmentEnds(Alignment alignment) {
        int SEQUENCE_WIDTH = 50;
        char[] sequence1 = alignment.getSequence1();
        char[] sequence2 = alignment.getSequence2();

        int length = sequence1.length > sequence2.length ? sequence2.length : sequence1.length;

        int oldPosition1, position1 = 1 + alignment.getStart1();
        int oldPosition2, position2 = 1 + alignment.getStart2();

        int line;

        char c1, c2;

        for (int i = 0; i * SEQUENCE_WIDTH < length; i++) {

            oldPosition1 = position1;
            oldPosition2 = position2;

            line = ((i + 1) * SEQUENCE_WIDTH) < length ? (i + 1) * SEQUENCE_WIDTH : length;

            for (int j = i * SEQUENCE_WIDTH, k = 0; j < line; j++, k++) {
                c1 = sequence1[j];
                c2 = sequence2[j];
                if (c1 == c2) {
                    position1++;
                    position2++;
                } else if (c1 == Alignment.GAP) {
                    position2++;
                } else if (c2 == Alignment.GAP) {
                    position1++;
                } else {
                    position1++;
                    position2++;
                }
            }
        }
        return new int[]{position1 - 1, position2 - 1};
    }
}
