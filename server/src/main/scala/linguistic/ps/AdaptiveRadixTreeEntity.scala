package linguistic.ps

import com.github.rohansuri.art.BinaryComparables

import java.util

object AdaptiveRadixTreeEntity {
  
  //https://github.com/rohansuri/adaptive-radix-tree
  //"com.github.rohansuri" % "adaptive-radix-tree" % "1.0.0-beta",

  /*NavigableMap<String, String>*/
  val art: util.NavigableMap[Integer, String] =
    new com.github.rohansuri.art.AdaptiveRadixTree[Integer, String](BinaryComparables.forInteger())

}
