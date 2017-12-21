package com.cloudera.bamreport;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public class BamCoverageSparkline {
  public static void main(String[] args) {
    String bam = args[0];
    Path bamPath = BamReport.asPath(URI.create(bam));

    int binSize = 1300000;

    Map<String, int[]> contigToBinCounts = BamCoverage.calculateCoverage(bamPath, binSize);
    int maxCount = BamCoverage.maxCount(contigToBinCounts);

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
