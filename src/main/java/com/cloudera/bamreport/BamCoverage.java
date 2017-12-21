package com.cloudera.bamreport;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class BamCoverage {

  public static Map<String, int[]> calculateCoverage(Path bamPath, int binSize) {
    SamReader reader = SamReaderFactory.makeDefault()
        .validationStringency(ValidationStringency.SILENT)
        .open(SamInputResource.of(bamPath));
    SAMFileHeader header = reader.getFileHeader();
    SAMFileHeader.SortOrder sortOrder = header.getSortOrder();

    if (sortOrder != SAMFileHeader.SortOrder.coordinate) {
      throw new IllegalArgumentException("BAM must be coordinate sorted.");
    }

    SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
    GenomeReference genomeReference = GenomeReference.inferReference(sequenceDictionary);
    if (genomeReference == null) {
      throw new IllegalArgumentException("Reference not recognized.");
    }

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

    return contigToBinCounts;
  }

  public static int maxCount(Map<String, int[]> contigToBinCounts) {
    int maxCount = 0;
    for (int[] bc : contigToBinCounts.values()) {
      maxCount = Math.max(maxCount, Arrays.stream(bc).max().getAsInt());
    }
    return maxCount;
  }
}
