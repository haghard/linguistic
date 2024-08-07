package linguistic.pruningRadixTrie;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * https://github.com/benldr/JPruningRadixTrie/blob/main/SourceCode/PruningRadixTrie.java
 * https://github.com/wolfgarbe/PruningRadixTrie?s=03
 * https://seekstorm.com/blog/pruning-radix-trie/
 */
public class JPruningRadixTrie {

  public long termCount = 0;
  public long termCountLoaded = 0;

  private static char delimiter = ':';

  //The trie
  private final Node trie;

  public JPruningRadixTrie() {
    this.trie = new Node(0);
  }

  // Insert a word into the trie
  public void addTerm(String term, long termFrequencyCount) {
    List<Node> nodeList = new ArrayList<>();
    addTerm(trie, term, termFrequencyCount, 0, 0, nodeList);
  }

  public void updateMaxCounts(List<Node> nodeList, long termFrequencyCount) {
    for (Node node : nodeList) {
      if (termFrequencyCount > node.getTermFrequencyCountChildMax()) {
        node.setTermFrequencyCountChildMax(termFrequencyCount);
      }
    }
  }

  public void addTerm(Node curr, String term, long termFrequencyCount, int id, int level, List<Node> nodeList) {
    try {
      nodeList.add(curr);

      //test for common prefix (with possibly different suffix)
      int common = 0;
      List<NodeChild> currChildren = curr.getChildren();
      if (currChildren != null) {
        for (int j = 0; j < currChildren.size(); j++) {
          String key = currChildren.get(j).getKey();
          Node node = currChildren.get(j).getNode();

          for (int i = 0; i < Math.min(term.length(), key.length()); i++) {
            if (term.charAt(i) == key.charAt(i)) {
              common = i + 1;
            } else {
              break;
            }
          }

          if (common > 0) {
            if ((common == term.length()) && (common == key.length())) {
              if (node.getTermFrequencyCount() == 0) {
                termCount++;
              }
              node.setTermFrequencyCount(node.getTermFrequencyCount() + termFrequencyCount);
              updateMaxCounts(nodeList, node.getTermFrequencyCount());
            } else if (common == term.length()) {
              //insert second part of oldKey as child
              Node child = new Node(termFrequencyCount);
              List<NodeChild> l = new ArrayList<>();
              l.add(new NodeChild(key.substring(common), node));
              child.setChildren(l);

              child.setTermFrequencyCountChildMax(
                Math.max(node.getTermFrequencyCountChildMax(), node.getTermFrequencyCount()));
              updateMaxCounts(nodeList, termFrequencyCount);

              //insert first part as key, overwrite old node
              currChildren.set(j, new NodeChild(term.substring(0, common), child));
              //sort children descending by termFrequencyCountChildMax to start lookup with most promising branch
              Collections.sort(currChildren, Comparator.comparing(
                (NodeChild e) -> e.getNode().getTermFrequencyCountChildMax()).reversed());
              //increment termcount by 1
              termCount++;
            } else if (common == key.length()) {
              addTerm(node, term.substring(common), termFrequencyCount, id, level + 1, nodeList);
            } else {
              Node child = new Node(0);
              List<NodeChild> l = new ArrayList<>();
              l.add(new NodeChild(key.substring(common), node));
              l.add(new NodeChild(term.substring(common), new Node(termFrequencyCount)));
              child.setChildren(l);

              child.setTermFrequencyCountChildMax(Math.max(node.getTermFrequencyCountChildMax(), Math.max(termFrequencyCount, node.getTermFrequencyCount())));
              updateMaxCounts(nodeList, termFrequencyCount);
              currChildren.set(j, new NodeChild(term.substring(0, common), child));

              Collections.sort(currChildren, Comparator.comparing((NodeChild e) -> e.getNode().getTermFrequencyCountChildMax()).reversed());
              termCount++;
            }
            return;
          }
        }
      }

      // initialize dictionary if first key is inserted
      if (currChildren == null) {
        List<NodeChild> l = new ArrayList<>();
        l.add(new NodeChild(term, new Node(termFrequencyCount)));
        curr.setChildren(l);
      } else {
        currChildren.add(new NodeChild(term, new Node(termFrequencyCount)));
        //sort children descending by termFrequencyCountChildMax to start lookup with most promising branch
        Collections.sort(currChildren, Comparator.comparing(
          (NodeChild e) -> e.getNode().getTermFrequencyCountChildMax()).reversed());
      }
      termCount++;
      updateMaxCounts(nodeList, termFrequencyCount);
    } catch (Exception e) {
      System.out.println("exception: " + term + " " + e.getMessage());
    }
  }

  public void findAllChildTerms(String prefix, int topK, String prefixString, List<TermAndFrequency> results, Boolean pruning) // Removed 3rd parameter: ref long termFrequencyCountPrefix
  {
    findAllChildTerms(prefix, trie, topK, prefixString, results, null, pruning);
  }

  public void findAllChildTerms(String prefix, Node curr, int topK, String prefixString, List<TermAndFrequency> results, BufferedWriter bw, Boolean pruning) {
    try {
      //pruning/early termination in radix trie lookup
      if (pruning && (topK > 0) && (results.size() == topK) &&
        (curr.getTermFrequencyCountChildMax() <= results.get(topK - 1).getTermFrequencyCount())) {
        return;
      }

      //test for common prefix (with possibly different suffix)
      Boolean noPrefix = (prefix.equals("") || prefix == null);

      if (curr.getChildren() != null) {
        for (NodeChild nodeChild : curr.getChildren()) {

          Node node = nodeChild.getNode();
          //pruning/early termination in radix trie lookup
          if (pruning && (topK > 0) && (results.size() == topK) &&
            (node.getTermFrequencyCount() <= results.get(topK - 1).getTermFrequencyCount()) &&
            (node.getTermFrequencyCountChildMax() <= results.get(topK - 1).getTermFrequencyCount())) {
            if (!noPrefix) {
              break;
            } else {
              continue;
            }
          }

          String key = nodeChild.getKey().trim();
          if (noPrefix || key.startsWith(prefix)) {
            if (node.getTermFrequencyCount() > 0) {
              // if (prefix == key) termfrequencyCountPrefix = node.getTermFrequencyCount();

              //candidate
              if (bw != null) {
                String line;
                if (key.length() > 0) {
                  line = (prefixString + key + delimiter + node.getTermFrequencyCount());
                } else {
                  line = (prefixString + delimiter + node.getTermFrequencyCount());
                }
                bw.append(line);
                bw.write('\n');
              } else {
                if (topK > 0) {
                  addTopKSuggestion(prefixString + key, node.getTermFrequencyCount(), topK, results);
                } else {
                  results.add(new TermAndFrequency(prefixString + key, node.getTermFrequencyCount()));
                }
              }
            }

            if ((node.getChildren() != null) && (node.getChildren().size() > 0)) {
              findAllChildTerms("", node, topK, prefixString + key, results, bw, pruning);
            }
            if (!noPrefix) {
              break;
            }
          } else if (prefix.startsWith(key)) {
            if ((node.getChildren() != null) && (node.getChildren().size() > 0)) {
              findAllChildTerms(prefix.substring(key.length()), node, topK, prefixString + key, results, bw, pruning);
            }
            break;
          }
        }
      }
    } catch (Exception e) {
      System.out.println("exception: " + prefix + " " + e.getMessage());
    }
  }

  public List<TermAndFrequency> getTopkTermsForPrefix(String prefix, int topK) {
    return getTopkTermsForPrefix(prefix, topK, true);
  }

  public List<TermAndFrequency> getTopkTermsForPrefix(String prefix, int topK, Boolean pruning) { // Removed parameter 'out long termFrequencyCountPrefix' as returning it in Java would mean changing the return type of the method.
    List<TermAndFrequency> results = new ArrayList<>();

    //termFrequency of prefix, if it exists in the dictionary (even if not returned in the topK results due to low termFrequency)
    // long termFrequencyCountPrefix = 0;

    // At the end of the prefix, find all child words
    findAllChildTerms(prefix, topK, "", results, pruning);

    return results;
  }

  public void writeTermsToFile(String path) {
    //save only if new terms were added
    File f = new File(path);
    if (!f.exists()) {
      try {
        f.createNewFile();
      } catch (IOException e) {
        System.err.println("Failed to create " + path);
      }
    }

    if (termCountLoaded == termCount) {
      return;
    }

    try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
      findAllChildTerms("", trie, 0, "", null, bw, true);
      System.out.println(termCount + " terms written.");
    } catch (Exception e) {
      System.out.println("Writing terms exception: " + e.getMessage());
    }
  }


  public Boolean readTermsFromFile(String path) { // Introduced parameter fieldDelimiter, the string on each line that separates the word from the frequency. Eg use value "\t" for tab delimited dictionary files.

    try (BufferedReader sr = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
      long startTime = System.currentTimeMillis();
      String line;

      //process a single line at a time only for memory efficiency
      while ((line = sr.readLine()) != null) {
        String[] lineParts = line.split(String.valueOf(delimiter));
        if (lineParts.length == 2) {
          try {
            long count = Long.parseUnsignedLong(lineParts[1]);
            this.addTerm(lineParts[0], count);
          } catch (NumberFormatException e) {
            System.out.println("Warning - frequency could not be extracted from a dictionary line. Skipping line.");
          }
        }
      }
      termCountLoaded = termCount;
      long elapsedMilliseconds = System.currentTimeMillis() - startTime;
      System.out.println(termCount + " terms loaded in " + elapsedMilliseconds + " ms");
    } catch (FileNotFoundException e) {
      System.out.println("Could not find file " + path);
      return false;
    } catch (Exception e) {
      System.out.println("Loading terms exception: " + e.getMessage());
    }

    return true;
  }

  public void addTopKSuggestion(String term, long termFrequencyCount, int topK, List<TermAndFrequency> results) {
    if ((results.size() < topK) || (termFrequencyCount >= results.get(topK - 1).getTermFrequencyCount())) {
      TermAndFrequency termAndFrequency = new TermAndFrequency(term, termFrequencyCount);
      int index = Collections.binarySearch(results, termAndFrequency, Comparator.comparing(
        (TermAndFrequency e) -> e.getTermFrequencyCount()).reversed()); // descending order
      if (index < 0) {
        results.add(~index, termAndFrequency);
      } else {
        results.add(index, termAndFrequency);
      }

      if (results.size() > topK) {
        results.remove(topK);
      }
    }
  }
}