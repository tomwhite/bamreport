package com.cloudera.bamreport;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.util.BufferedLineReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

// Inspired by Hail's GenomeReference
public class GenomeReference {

  public static final GenomeReference GRCh37 = new GenomeReference("GRCh37", "human_g1k_v37.dict");
  public static final GenomeReference GRCh38 = new GenomeReference("GRCh38", "Homo_sapiens_assembly38.dict");

  private static final GenomeReference[] WELL_KNOWN_GENOMES = new GenomeReference[] {
      GRCh37,
      GRCh38
  };

  private final String name;
  private final SAMSequenceDictionary dictionary;

  private GenomeReference(String name, String dictResource) {
    this.name = name;
    try {
      SAMTextHeaderCodec codec = new SAMTextHeaderCodec();
      String dict = getResourceFileAsString(dictResource);
      SAMFileHeader header = codec.decode(BufferedLineReader.fromString(dict), getClass().getSimpleName());
      this.dictionary = header.getSequenceDictionary();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getName() {
    return name;
  }

  public int getLength(String contig) {
    return dictionary.getSequence(contig).getSequenceLength();
  }

  /**
   * Check if the given sequence dictionary is from this genome reference by comparing
   * names and contig lengths. If {@code sequenceDictionary} has a sequence with a
   * different name or length then return {@code false}.
   * @param sequenceDictionary
   * @return if the genome reference contains the given sequence dictionary
   */
  public boolean contains(SAMSequenceDictionary sequenceDictionary) {
    for (SAMSequenceRecord seq : sequenceDictionary.getSequences()) {
      SAMSequenceRecord genomeSequence = dictionary.getSequence(seq.getSequenceName());
      if (genomeSequence == null || (genomeSequence.getSequenceLength() != seq.getSequenceLength())) {
        return false;
      }
    }
    return true;
  }

  public static GenomeReference inferReference(SAMSequenceDictionary sequenceDictionary) {
    for (GenomeReference genomeReference : WELL_KNOWN_GENOMES) {
      if (genomeReference.contains(sequenceDictionary)) {
        return genomeReference;
      }
    }
    return null;
  }

  private static String getResourceFileAsString(String resourceFileName) throws IOException {
    try (InputStream is = GenomeReference.class.getClassLoader().getResourceAsStream(resourceFileName)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }
}
