package com.cloudera.bamreport;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordFactory;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class BamReport {

  public static Path asPath(URI uri) {
    if (uri.getScheme() == null) {
      return Paths.get(uri.toString());
    }
    try {
      return Paths.get(uri);
    } catch (FileSystemNotFoundException e) {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) {
        throw e;
      }
      try {
        return FileSystems.newFileSystem(uri, new HashMap<>(), cl).provider().getPath(uri);
      } catch (IOException ex) {
        throw new RuntimeException("Cannot create filesystem for " + uri, ex);
      }
    }
  }

  public static void main(String[] args) {
    String bam = args[0];
    Path bamPath = asPath(URI.create(bam));
    SamReader reader = SamReaderFactory.makeDefault().open(SamInputResource.of(bamPath));
    SAMFileHeader header = reader.getFileHeader();
    SAMFileHeader.SortOrder sortOrder = header.getSortOrder();
    System.out.printf("BAM file: %s\n", bamPath);
    System.out.printf("Version: %s\n", header.getVersion());
    System.out.printf("Sort order: %s\n", sortOrder);

    SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
    if (sequenceDictionary.size() == 0) {
      System.out.println("No sequence dictionary");
    } else {
      String inferredRef = GenomeReference.inferReference(sequenceDictionary);
      if (inferredRef == null) {
        System.out.println("Sequence dictionary:");
      } else {
        System.out.printf("Sequence dictionary (%s inferred):\n", inferredRef);
      }
      for (SAMSequenceRecord seq : sequenceDictionary.getSequences()) {
        System.out.printf("\tSN: %s, LN: %s\n", seq.getSequenceName(), seq.getSequenceLength());
      }
    }

    System.out.println("Read groups:");
    List<SAMReadGroupRecord> readGroups = header.getReadGroups();
    for (SAMReadGroupRecord readGroup : readGroups) {
      System.out.printf("\tID: %s, Sample (SM): %s\n", readGroup.getId(), readGroup.getSample());
    }

    SAMRecordIterator iterator = reader.iterator();
    if (iterator.hasNext()) {
      SAMRecord firstRead = iterator.next();
      System.out.println("First read:");
      System.out.printf("\tRead-Name: %s\n", firstRead.getReadName());
      if (firstRead.getReadUnmappedFlag()) {
        System.out.printf("\tRead unmapped\n");
      } else {
        System.out.printf("\tStart: %s\n", firstRead.getAlignmentStart());
        System.out.printf("\tEnd: %s\n", firstRead.getAlignmentEnd());
      }
      System.out.printf("\tRead-Length: %s\n", firstRead.getReadLength());
    } else {
      System.out.println("No reads");
    }
  }


}
