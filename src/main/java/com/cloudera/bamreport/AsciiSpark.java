package com.cloudera.bamreport;

import java.util.Arrays;
import java.util.List;

// based on https://github.com/RichardWarburton/jspark
public class AsciiSpark {

  private static final List<Character> ticks = Arrays.asList('\u2581','\u2582', '\u2583', '\u2584', '\u2585', '\u2586', '\u2587','\u2588');

  public static String asciiGraph(int[] values, int max) {
    final int min = 0;
    final float scale = (max - min) / 7f;
    final StringBuilder accumulator = new StringBuilder();
    for (final Integer value : values) {
      final int index = Math.round((value - min) / scale);
      accumulator.append(ticks.get(index));
    }
    return accumulator.toString();
  }
}
