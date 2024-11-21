package cache.util;

import com.caucho.hessian.client.HessianProxyFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import cache.interfaces.AdminService;
import cache.interfaces.AlignmentService;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Manjunath Kustagi
 */
public class AlignmentServiceTest {

    public static void main(String[] args) {
        try {
            String baseURL = "https://parclip.rockefeller.edu/server_3/AnvesanaWS/";
            System.setProperty("base.url", baseURL.trim());

            final String authUser = "mkustagi";
            final String authPassword = "vxI2L9Qt";
            Authenticator.setDefault(new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            authUser, authPassword.toCharArray());
                }
            });

            System.setProperty("http.proxyUser", authUser);
            System.setProperty("http.proxyPassword", authPassword);

            String url = System.getProperty("base.url") + "AdminService";
            HessianProxyFactory factory = new HessianProxyFactory();
            AdminService adminService = (AdminService) factory.create(AdminService.class, url);
            url = System.getProperty("base.url") + "AlignmentService";
            factory = new HessianProxyFactory();
            AlignmentService alignmentService = (AlignmentService) factory.create(AlignmentService.class, url);
//            Long[] ids = new Long[] {26129l, 26415l, 26469l, 26471l, 26473l, 26476l, 26487l, 26500l, 26501l, 26502l, 26503l, 26590l};
//            Long[] ids = new Long[] {26604l, 26738l, 26739l, 26751l, 26877l, 26883l, 27620l, 27796l, 27886l, 27896l, 28026l, 28027l, 28344l, 28608l, 
//                28615l, 28617l, 28717l, 29071l, 29605l, 29688l, 29829l, 29867l, 30014l, 30069l, 30133l, 30220l, 30245l, 30276l, 
//                30277l, 30279l, 30280l, 30285l, 30286l, 30289l, 30292l, 30293l, 30298l, 30303l, 30305l, 30307l, 30312l, 30314l, 
//                30316l, 30318l, 30319l, 30321l, 30322l, 30323l, 30326l, 30328l, 30347l, 30356l, 30357l, 30359l, 30371l, 30373l, 
//                30378l, 30380l, 30381l, 30405l, 30406l, 30409l, 30410l, 30415l, 30416l, 30435l, 30438l, 30451l, 30455l, 30456l, 
//                30461l, 30462l, 30467l, 30468l, 30472l, 30490l, 30494l, 30495l, 30496l, 30515l, 30516l, 30525l, 30538l, 30540l, 
//                30549l, 30551l, 30602l, 30605l, 30606l, 30608l, 30613l, 30620l, 30621l, 30635l, 30654l, 30660l, 30670l, 30679l, 
//            Long[] ids = new Long[]{30684l, 30685l, 30729l, 30740l, 30754l, 30755l, 30757l, 30764l, 30787l, 30910l, 30924l, 30930l, 30951l, 30986l,
//            Long[] ids = new Long[]{30988l, 31014l, 31015l, 31022l, 31062l, 31090l, 31098l, 31100l, 31105l, 31129l, 31136l, 31145l, 31147l,
//            Long[] ids = new Long[]{31147l,
//                31162l, 31165l, 31179l, 31180l, 31188l};
            Long[] ids = new Long[]{31199l};
            List<Long> tids = new ArrayList<>();
            tids.addAll(Arrays.asList(ids));
//            tids.add(29441l);
//            tids.add(29442l);
//            tids.add(29443l);
//            tids.add(29444l);
//            tids.add(29114l);
//            ids = new Long[]{1l, 3l, 5l, 6l, 7l, 9l, 10l, 11l, 12l, 13l, 14l, 15l, 16l, 17l, 19l, 22l, 25l, 28l, 29l, 30l, 31l, 32l, 33l, 34l, 35l, 36l, 37l, 38l, 39l, 40l, 41l, 42l, 43l, 44l, 45l, 46l, 47l, 48l, 49l, 55l, 56l, 57l, 59l, 63l, 64l, 65l, 66l, 67l, 69l, 70l, 71l, 72l, 73l, 74l, 75l, 76l, 77l, 78l, 79l, 80l, 102l, 103l, 104l, 105l, 117l, 118l, 119l, 120l, 121l, 122l, 123l, 124l, 125l, 126l, 127l, 128l, 129l, 132l, 133l, 134l, 135l, 149l, 150l, 151l, 152l, 153l, 155l, 156l, 157l, 158l, 161l, 162l, 163l, 164l, 165l, 166l, 167l, 168l, 169l, 171l, 172l, 173l, 174l, 175l, 176l, 177l, 178l, 179l, 180l, 183l, 184l, 185l, 191l, 192l, 193l, 194l, 195l, 203l, 206l, 207l, 211l, 212l, 213l, 214l, 220l, 221l, 222l, 223l, 224l, 225l, 226l, 227l, 228l, 229l, 240l, 241l, 242l, 243l, 244l, 245l, 246l, 247l, 248l, 249l, 266l, 267l, 268l, 269l, 274l, 275l, 276l, 277l, 278l, 279l, 280l, 281l, 287l, 289l, 291l, 292l, 293l, 294l, 295l, 296l, 297l, 298l, 299l, 300l, 301l, 302l, 303l, 304l, 305l, 306l, 307l, 308l, 309l, 310l, 314l, 315l, 316l, 317l, 318l, 321l, 322l, 323l, 325l, 340l, 341l, 342l, 343l, 344l, 345l, 346l, 347l, 348l, 349l, 350l, 351l, 352l, 353l, 354l, 355l, 356l, 357l, 358l, 359l, 360l, 361l, 362l, 363l, 364l, 365l, 370l, 371l, 372l, 373l, 374l, 375l, 376l, 377l, 378l, 379l, 380l, 381l, 382l, 383l, 387l, 388l, 389l, 390l, 391l, 392l, 411l, 412l, 413l, 414l, 415l, 416l, 417l, 418l, 419l, 420l, 421l, 422l, 423l, 424l, 425l, 428l, 429l, 430l, 431l, 432l, 434l, 435l, 436l, 437l, 438l, 439l, 440l, 441l, 442l, 443l, 444l, 445l, 446l, 447l, 448l, 449l, 450l, 451l, 452l, 453l, 454l, 455l, 456l, 457l, 459l, 460l, 461l, 462l, 463l, 464l, 465l, 466l, 467l, 468l, 469l, 470l, 471l, 472l, 473l, 474l, 475l, 476l, 477l, 478l, 479l, 480l, 481l, 482l, 483l, 484l, 485l, 486l, 487l, 488l, 489l, 490l, 491l, 492l, 493l, 494l, 495l, 496l, 497l, 498l, 499l, 500l, 501l, 502l, 503l, 504l, 505l, 506l, 507l, 508l, 509l, 510l, 511l, 512l, 513l, 514l, 515l, 516l, 517l, 518l, 519l, 520l, 521l, 522l, 523l, 524l, 526l, 527l, 528l, 529l, 530l, 531l, 532l, 533l, 534l, 535l, 536l, 537l, 538l, 539l, 540l, 541l, 542l, 543l, 544l, 546l, 547l, 548l, 549l, 550l, 551l, 552l, 553l, 554l, 555l, 556l, 557l, 558l, 559l, 560l, 561l, 562l, 563l, 564l, 565l, 566l, 567l, 568l, 569l, 570l, 571l, 572l, 573l, 574l, 575l, 576l, 577l, 578l, 579l, 580l, 581l, 583l, 584l, 585l, 586l, 587l, 588l, 589l, 590l, 591l, 592l, 593l, 595l, 596l, 597l, 598l, 599l, 600l, 601l, 602l, 603l, 604l, 605l, 606l, 607l, 608l, 609l, 610l, 611l, 612l, 613l, 614l, 615l, 616l, 617l, 618l, 619l, 620l, 621l, 622l, 623l, 624l, 625l, 626l, 627l, 628l, 629l, 630l, 631l, 632l, 633l, 634l, 635l, 636l, 637l, 638l, 639l, 640l, 641l, 642l, 643l, 644l, 645l, 646l, 647l, 648l, 649l, 650l, 651l, 652l, 653l, 654l, 655l, 656l, 657l, 658l, 659l, 660l, 661l, 662l, 663l, 664l, 665l, 666l, 667l, 668l, 669l, 670l, 671l, 672l, 673l, 674l, 675l, 676l, 677l, 678l, 679l, 680l, 681l, 682l, 683l, 684l, 685l, 686l, 687l, 688l, 689l, 690l, 691l, 692l, 693l, 694l, 695l, 696l, 697l, 698l, 699l, 700l, 701l, 702l, 703l, 704l, 705l, 706l, 707l, 708l, 709l, 710l, 711l, 712l, 713l, 714l, 715l, 716l, 717l, 718l, 719l, 720l, 721l, 722l, 723l, 724l, 725l, 726l, 727l, 728l, 729l, 730l, 731l, 732l, 733l, 734l, 735l, 736l, 737l, 738l, 739l, 740l, 741l, 742l, 743l, 744l, 745l, 746l, 747l, 748l, 749l, 750l, 751l, 752l, 753l, 754l, 755l, 756l, 757l, 758l, 759l, 760l, 761l, 762l, 763l, 764l, 765l, 766l, 767l, 768l, 769l, 770l, 771l, 772l, 773l, 774l, 775l, 776l, 777l, 778l, 779l, 780l, 781l, 782l, 783l, 784l, 785l, 786l, 787l, 788l, 789l, 790l, 791l, 792l, 793l, 794l, 795l, 796l, 797l, 799l, 800l, 801l, 802l, 803l, 804l, 805l, 806l, 807l, 808l, 809l, 810l, 811l, 812l, 813l, 814l, 815l, 816l, 817l, 818l, 819l, 820l, 821l, 822l, 823l, 824l, 825l, 826l, 827l, 828l, 829l, 830l, 831l, 832l, 833l, 834l, 835l, 836l, 837l, 838l, 839l, 840l, 841l, 842l, 843l, 844l, 845l, 846l, 847l, 848l, 849l, 850l, 851l, 852l, 853l, 854l, 855l, 856l, 857l, 858l, 859l, 860l, 861l, 862l, 863l, 864l, 865l, 961l, 1007l, 1008l, 1009l, 1010l, 1011l, 1012l, 1013l, 1014l, 1015l, 1016l, 1017l, 1018l, 1019l, 1020l, 1021l, 1022l, 1023l, 1024l, 1025l, 1026l, 1027l, 1028l, 1029l, 1030l, 1031l, 1032l, 1033l, 1034l, 1035l, 1036l, 1037l, 1038l, 1039l, 1040l, 1041l, 1042l, 1043l, 1044l, 1046l, 1047l, 1048l, 1049l, 1050l, 1051l, 1052l, 1053l, 1054l, 1055l, 1056l, 1057l, 1058l, 1059l, 1060l, 1061l, 1062l, 1063l, 1064l, 1065l, 1066l, 1067l, 1068l, 1069l, 1070l, 1071l, 1072l, 1073l, 1074l, 1075l, 1076l, 1077l, 1078l, 1079l, 1080l, 1081l, 1082l, 1083l, 1084l, 1085l, 1086l, 1087l, 1088l, 1089l, 1090l, 1091l, 1092l, 1093l, 1094l, 1095l, 1096l, 1097l, 1098l, 1099l, 1100l, 1101l, 1102l, 1103l, 1104l, 1105l, 1106l, 1107l, 1108l, 1109l, 1110l, 1111l, 1112l, 1113l, 1114l, 1115l, 1116l, 1117l, 1118l, 1123l, 1124l, 1125l, 1126l, 1127l, 1128l, 1129l, 1130l, 1131l, 1132l, 1133l, 1134l, 1135l, 1136l, 1137l, 1138l, 1139l, 1140l, 1141l, 1142l, 1143l, 1144l, 1145l, 1146l, 1147l, 1148l, 1149l, 1150l, 1151l, 1152l, 1153l, 1154l, 1155l, 1156l, 1157l, 1158l, 1159l, 1160l, 1200l, 1201l, 1202l, 1203l, 1204l, 1205l, 1206l, 1207l, 1208l, 1209l, 1210l, 1211l, 1212l, 1213l, 1214l, 1215l, 1216l, 1217l, 1218l, 1219l};
//            List<Long> eids = new ArrayList<>();
//            eids.addAll(Arrays.asList(ids));
//            List<List<SummaryResults>> allSummaries = new ArrayList<>();
//            BufferedReader br = new BufferedReader(new FileReader(new File("eids.txt")));
//            String line = "";
//            List<Long> eids = new ArrayList<>();
//            List<Integer> distances = new ArrayList<>();
//            System.out.println("Loaded eids..");
//
//            while ((line = br.readLine()) != null) {
//                String[] tokens = line.split("\t");
//                Long eid = Long.parseLong(tokens[1]);
//                Integer d = Integer.parseInt(tokens[2]);
//                List<SummaryResults> srList = adminService.getSummary(Integer.parseInt(tokens[0]), eid, d);
//                allSummaries.add(srList);
//                eids.add(eid);
//                distances.add(d);
//                System.out.println("eid: " + eid + ", results size: " + srList.size());
//            }
//
//            System.out.println("Loaded summaries..");
//            
//            List<SummaryResults> comparisons = new ArrayList<>();
//
//            final Comparator<SummaryResults> comparator1 = (SummaryResults t1, SummaryResults t2) -> t2.geneId.compareTo(t1.geneId);
//
//            allSummaries.stream().forEach((list) -> {
//                Collections.sort(list, comparator1);
//            });
//
//            System.out.println("Sorted summaries..");

//            allSummaries.get(0).stream().map((sr) -> {
//                SummaryResults comparison = new SummaryResults();
//                comparison.geneId = sr.geneId;
//                comparison.geneSymbol = sr.geneSymbol;
//                allSummaries.stream().forEach((l) -> {
//                    int index = Collections.binarySearch(l, sr, comparator1);
//                    if (index >= 0) {
//                        SummaryResults sr1 = l.get(index);
//                        comparison.otherRpkms.add(sr1.rpkm);
//                        comparison.otherRanks.add(sr1.rpkmRank);
//                    } else {
//                        comparison.otherRpkms.add(0d);
//                        comparison.otherRanks.add(l.size() + 1);
//                    }
//                });
//                double variance = StatUtil.computeVariance(comparison.otherRpkms);
//                double entropy = StatUtil.computeShannonEntropy(comparison.otherRpkms);
//                comparison.rankVariance = variance;
//                comparison.rankEntropy = entropy;
//                comparison.biotype = sr.biotype;
//                comparison.levelIIclassification = sr.levelIIclassification;
//                comparison.levelIfunction = sr.levelIfunction;
//                return comparison;
//            }).forEach((comparison) -> {
//                comparisons.add(comparison);
//            });
//
//            System.out.println("Created comparisons..");
//
//            Comparator<SummaryResults> comparator2 = (SummaryResults t1, SummaryResults t2) -> new Double(t2.rankEntropy).compareTo(t1.rankEntropy);
//
//            Collections.sort(comparisons, comparator2);
//
//            System.out.println("Sorted comparisons..");
//
//            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("specificity.txt")));
//
//            bw.write("Gene Ensembl ID");
//            bw.write("\t" + "Gene Symbol");
//            bw.write("\t" + "Biotypes");
//            bw.write("\t" + "Level II classification");
//            bw.write("\t" + "Level I function");
//
//            for (int i = 0; i < eids.size(); i++) {
//                bw.write("\t" + "Rank_" + eids.get(i) + ":" + distances.get(i));
//            }
//
//            bw.write("\t" + "Variance");
//            bw.write("\t" + "Entropy");
//
//            for (int i = 0; i < eids.size(); i++) {
//                bw.write("\t" + "RPKM_" + eids.get(i) + ":" + distances.get(i));
//            }
//
//            bw.newLine();
//
//            for (SummaryResults sr : comparisons) {
//                bw.write(sr.geneId);
//                bw.write("\t" + sr.geneSymbol);
//                bw.write("\t" + sr.biotype);
//                bw.write("\t" + sr.levelIIclassification);
//                bw.write("\t" + sr.levelIfunction);
//                for (Integer rank : sr.otherRanks) {
//                    bw.write("\t" + rank);
//                }
//                bw.write("\t" + sr.rankVariance);
//                bw.write("\t" + sr.rankEntropy);
//                for (Double rpkm : sr.otherRpkms) {
//                    bw.write("\t" + rpkm);
//                }
//                bw.newLine();
//            }
//
//            System.out.println("Wrote specificities..");            
//            alignmentService.runAlignment(31049, 976, 0);
//            alignmentService.runAlignment(31052, 1175, 0);
//            alignmentService.editTranscriptName(31199l, "G6PC_retainedintron_1_2_CDS_882_KM_ND_MK", "72a97973cbaca5f591fe2ad06feae5c0");
//            alignmentService.deleteTranscript(30979l, "72a97973cbaca5f591fe2ad06feae5c0");
//            alignmentService.editTranscriptName(29063l, "ABHD5_3ext", "72a97973cbaca5f591fe2ad06feae5c0");
//            long[] ids = {26943l, 26935l, 26936l, 26937l, 26938l, 26939l, 26940l, 26941l}; 
//            List<Long> tids = new ArrayList<>();
//            tids.add(31175l);
//            long[] ids = {27687l};
//            for (long tid : ids) {
//                System.out.println("transcript Id: " + tid);
//                alignmentService.deleteTranscript(tid, "72a97973cbaca5f591fe2ad06feae5c0");
//                tids.add(tid);
//            }
//
            alignmentService.runAlignmentWithMismatches(tids);
//            alignmentService.runAlignmentWithList(tids);
//            alignmentService.runAlignmentWithLists(tids, eids);
//            BufferedReader br = new BufferedReader(new FileReader(new File("extension.txt")));
//            String line = "";
//            final AlignmentWorker worker = new AlignmentWorker();
//            worker.setExperimentID(30, 1177, 0);
//            while ((line = br.readLine()) != null) {
//                TranscriptMappingResults tmr1 = new TranscriptMappingResults();
//                String[] tokens = line.split("\t");
//                String name = tokens[0];
//                Long transcriptId = Long.parseLong(name.split("_")[0]);
//                List<AnnotationResults> arList = worker.getAnnotationsForTranscript(transcriptId);
//                String transcript = tokens[1];
//                if (transcript != null) {
//                    transcript = transcript.toUpperCase();
//                    transcript = transcript.replaceAll("[^\\w\\s]", "").trim();
//                    transcript = transcript.replaceAll("[^\\p{L}\\p{Z}]", "").trim();
//                }
//                tmr1.mappedAlignments = transcript;
//                tmr1.transcriptLength = tmr1.mappedAlignments.length();
//                tmr1.name = "CPS1_prom2_CDS_3ext_720kb_1177_MK";
//                tmr1.symbol = "CPS1_prom2_CDS_3ext_720kb_1177_MK";
//                System.out.println(tmr1.transcriptID + "\t" + tmr1.name + "\t" + tmr1.transcriptLength);
//                worker.persistEditTranscript(tmr1, arList, 1177, false, "72a97973cbaca5f591fe2ad06feae5c0", "mkustagi@gmail.com");
//            }
//            br.close();
//            BufferedReader br = new BufferedReader(new FileReader(new File("/Users/mk2432/Desktop/splicing/v5/tid_eid.txt")));
//            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("counts.txt")));
//            String line = "";
//            AlignmentWorker worker = new AlignmentWorker();
//            while ((line = br.readLine()) != null) {
//                System.out.println("Processing line: " + line);
//                String[] tokens = line.split("\t");
//                Long eid = Long.parseLong(tokens[1]);
//                Long tid = Long.parseLong(tokens[0]);
//
//                worker.setExperimentID(0, eid, 0);
//                TranscriptMappingResults tmr = new TranscriptMappingResults();
//                tmr.transcriptID = tid;
//                tmr = worker.populateAlignmentDisplay(tmr);
//                Map<String, List<Integer>> coverages = tmr.coverages;
//                List<Integer> coverageTotal0 = coverages.get("coverageTotal");
//                int size = coverageTotal0.size();
//                System.out.println("coverageTotal0: " + coverageTotal0.toString());
//                List<Integer> coverageTotalRC0 = coverages.get("coverageTotalRC");
//                System.out.println("coverageTotalRC0: " + coverageTotalRC0.toString());
//                int sizeRC = coverageTotalRC0.size();
//                if (size == 0) {
//                    size = sizeRC;
//                    coverageTotal0 = new ArrayList<>(size);
//                    for (int i = 0; i < size; i++) {
//                        coverageTotal0.set(i, 0);
//                    }
//                }
//                worker.setExperimentID(0, eid, 1);
//                tmr = new TranscriptMappingResults();
//                tmr.transcriptID = tid;
//                tmr = worker.populateAlignmentDisplay(tmr);
//                coverages = tmr.coverages;
//                List<Integer> coverageTotal1 = coverages.get("coverageTotal");
//                size = coverageTotal0.size();
//                List<Integer> coverageTotalRC1 = coverages.get("coverageTotalRC");
//                sizeRC = coverageTotalRC1.size();
//                if (size == 0) {
//                    size = sizeRC;
//                    coverageTotal1 = new ArrayList<>(size);
//                    for (int i = 0; i < size; i++) {
//                        coverageTotal1.set(i, 0);
//                    }
//                }
//                worker.setExperimentID(0, eid, 2);
//                tmr = new TranscriptMappingResults();
//                tmr.transcriptID = tid;
//                tmr = worker.populateAlignmentDisplay(tmr);
//                coverages = tmr.coverages;
//                List<Integer> coverageTotal2 = coverages.get("coverageTotal");
//                size = coverageTotal2.size();
//                List<Integer> coverageTotalRC2 = coverages.get("coverageTotalRC");
//                sizeRC = coverageTotalRC2.size();
//                if (size == 0) {
//                    size = sizeRC;
//                    coverageTotal2 = new ArrayList<>(size);
//                    for (int i = 0; i < size; i++) {
//                        coverageTotal2.set(i, 0);
//                    }
//                }
//                List<Integer> coverage = new ArrayList<>(coverageTotal0.size());
//                coverageTotal0.stream().forEach((_item) -> {
//                    coverage.add(0);
//                });
//
//                for (int i = 0; i < coverageTotal0.size(); i++) {
//                    coverage.set(i, coverageTotal0.get(i) - coverageTotalRC0.get(i)
//                            + coverageTotal1.get(i) - coverageTotalRC1.get(i)
//                            + coverageTotal2.get(i) - coverageTotalRC2.get(i));
//                }
//
//                List<Double> densities = new ArrayList<>();
//                
//                for (int i = 0; i < coverage.size() - 100; i++) {
//                    double sum = 0;
//                    for (int j = 0; j < 100; j++) {
//                        sum += coverage.get(i + j);
//                    }
//                    double density = sum / 100;
//                    densities.add(density);
//                }
//                
//                OptionalDouble avg = densities.stream().mapToDouble(i -> i).average();
//                
//                bw.write(tid + "\t" + eid + "\t" + avg.getAsDouble());
//                bw.newLine();
//                System.out.println(tid + "\t" + eid + "\t" + avg.getAsDouble());
//            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(AlignmentServiceTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AlignmentServiceTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
