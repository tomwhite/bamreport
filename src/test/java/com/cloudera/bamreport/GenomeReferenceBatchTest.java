package com.cloudera.bamreport;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;

public class GenomeReferenceBatchTest {
  @Test
  public void testGatkBams() throws IOException {
    List<String> filenames = Files.readAllLines(Paths.get("src/test/resources/gatk_bams.txt"));
    int totalCount = 0;
    int norefCount = 0;
    int grch37Count = 0;
    int grch38Count = 0;
    for (String filename : filenames) {
      try (SamReader reader = SamReaderFactory.makeDefault().open(new File(filename))) {
        SAMFileHeader header = reader.getFileHeader();
        SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
        String ref = GenomeReference.inferReference(sequenceDictionary);
        if (ref == null) {
          norefCount++;
          System.out.println("?," + filename);
        } else if (ref.equals("GRCh37")) {
          grch37Count++;
          System.out.println(ref + "," + filename);
        } else if (ref.equals("GRCh38")) {
          grch38Count++;
          System.out.println(ref + "," + filename);
        }
        totalCount++;
      }
    }
    System.out.println("Total: " + totalCount);
    System.out.println("No ref: " + norefCount);
    System.out.println("GRCh37: " + grch37Count);
    System.out.println("GRCh38: " + grch38Count);
  }
}
