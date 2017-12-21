package com.cloudera.bamreport;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class BamCoverageSparkline {
  public static void main(String[] args) {
    String bam = args[0];
    Path bamPath = BamReport.asPath(URI.create(bam));
    SamReader reader = SamReaderFactory.makeDefault()
        .validationStringency(ValidationStringency.SILENT)
        .open(SamInputResource.of(bamPath));
    SAMFileHeader header = reader.getFileHeader();
    SAMFileHeader.SortOrder sortOrder = header.getSortOrder();

    if (sortOrder != SAMFileHeader.SortOrder.coordinate) {
      System.err.println("BAM must be coordinate sorted.");
      System.exit(1);
    }

    SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
    GenomeReference genomeReference = GenomeReference.inferReference(sequenceDictionary);
    if (genomeReference == null) {
      System.err.println("Reference not recognized.");
      System.exit(1);
    }

    int binSize = 1300000;
    int maxCount = 0;

    String currentContig = null;
    int[] binCounts = null; // count reads that start in a particular bin
    Map<String, int[]> contigToBinCounts = new LinkedHashMap<>();
    for (SAMRecord read : reader) {
      if (!read.getReadUnmappedFlag()) {
        String contig = read.getContig();
        if (!contig.equals(currentContig)) { // new contig
          if (currentContig != null) {
            contigToBinCounts.put(currentContig, binCounts);
          }
          currentContig = contig;
          int currentContigLength = genomeReference.getLength(contig);
          binCounts = new int[1 + currentContigLength / binSize];
        }
        int start = read.getAlignmentStart();
        binCounts[start / binSize]++;
      }
    }
    if (binCounts != null) {
      contigToBinCounts.put(currentContig, binCounts);
    }

    for (int[] bc : contigToBinCounts.values()) {
      maxCount = Math.max(maxCount, Arrays.stream(bc).max().getAsInt());
    }

    writeHtml(contigToBinCounts, maxCount);

  }

  private static void writeHtml(Map<String, int[]> contigToBinCounts, int maxCount) {
    System.out.println(
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \n" +
        "    \"http://www.w3.org/TR/html4/strict.dtd\">\n" +
        "<html>\n" +
        "<head>\n" +
        "  <title>BAM coverage sparklines</title>\n" +
        "</head>\n" +
        "\n" +
        "<body>\n" +
        "\n" +
        "<script type=\"text/javascript\" src=\"https://code.jquery" +
        ".com/jquery-1.7.2.min.js\"></script>\n" +
        "<script type=\"text/javascript\" src=\"jquery.sparkline.min.js\"></script>\n" +
        "<script type=\"text/javascript\">\n" +
        "$(function() {"
    );
    for (Map.Entry<String, int[]> entry : contigToBinCounts.entrySet()) {
      String contig = entry.getKey();
      int[] bins = entry.getValue();
      System.out.printf("$(\"#chr%s\").sparkline(", contig);
      System.out.print("[");
      for (int i = 0; i < bins.length; i++) {
        System.out.print(bins[i]);
        if (i < bins.length - 1) {
          System.out.print(",");
        }
      }
      System.out.println("], {");
      System.out.printf("\ttype: 'line',\n" +
          "\twidth: '%spx',\n" +
          "\tchartRangeMax: %s,\n" +
          "\tspotColor: false,\n" +
          "\tminSpotColor: false,\n" +
          "\tmaxSpotColor: false});\n", bins.length, maxCount);
    }
    System.out.println(
        "});\n" +
        "</script>\n" +
        "\n" +
        "<table>"
    );

    System.out.println("\t<tr>");
    for (String contig : contigToBinCounts.keySet()) {
      System.out.printf("\t\t<td><span id=\"chr%s\"></span></td>\n", contig);
    }
    System.out.println("\t</tr>");

    System.out.println("\t<tr>");
    for (String contig : contigToBinCounts.keySet()) {
      System.out.printf("\t\t<td>%s</td>\n", contig);
    }
    System.out.println("\t</tr>");

    System.out.println(
        "</table>\n" +
        "</body>\n" +
        "\n" +
        "</html>"
    );
  }

}
