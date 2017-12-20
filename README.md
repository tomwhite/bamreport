
```
BAM=/Users/tom/workspace/gatk/src/test/resources/large/CEUTrio.HiSeq.WGS.b37.NA12878.20.21.bam
~/sw/bedtools2/bin/bedtools genomecov -ibam $BAM -bga > bam.bedgraph
```

From http://seqanswers.com/forums/showpost.php?p=116995&postcount=11

```
DICT=/Users/tom/workspace/gatk/src/test/resources/large/human_g1k_v37.20.21.dict
grep '@SQ' $DICT | tr ':' '\t' | awk -v OFS='\t' '{print $3, $5}' > $DICT.genome
~/sw/bedtools2/bin/bedtools makewindows -g $DICT.genome -w 1000000 \
    | ~/sw/bedtools2/bin/bedtools coverage -a - -b $BAM -counts > $BAM.counts.txt
````


bedtools coverage -hist -abam $BAM

----

```bash
mvn compile
mvn exec:java  -Dexec.mainClass=com.cloudera.bamreport.BamReport -Dexec.args="/Users/tom/workspace/gatk/src/test/resources/large/CEUTrio.HiSeq.WGS.b37.NA12878.20.21.bam"


# unmapped BAM
mvn exec:java  -Dexec.mainClass=com.cloudera.bamreport.BamReport -Dexec.args="/Users/tom/workspace/gatk/src/test/resources/org/broadinstitute/hellbender/metrics/analysis/QualityScoreDistribution/unmapped.bam"
```