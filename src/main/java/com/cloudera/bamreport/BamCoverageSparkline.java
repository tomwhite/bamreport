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

    int binSize = 1000000;

    String currentContig = null;
    int[] binCounts = null; // count reads that start in a particular bin
    for (SAMRecord read : reader) {
      if (!read.getReadUnmappedFlag()) {
        String contig = read.getContig();
        if (!contig.equals(currentContig)) { // new contig
          if (currentContig != null) {
            writeBins(currentContig, binCounts, binSize);
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
      writeBins(currentContig, binCounts, binSize);
    }

  }

  private static void writeBins(String contig, int[] bins, int binSize) {
    for (int i = 0; i < bins.length; i++) {
      System.out.printf("%s\t%s\t%s\t%s\n", contig, i * binSize, (i + 1) * binSize, bins[i]);
    }
  }

}
