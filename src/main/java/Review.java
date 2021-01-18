import com.opencsv.bean.CsvBindByName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.tuple.Pair;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review {

  private static List<String> PRIMITIVE_EMOTIONS =
      Arrays.asList("admiration", "anger", "disgust", "fear", "interest", "joy", "sadness",
          "surprise");

  /**
   * The review raw text.
   */
  @CsvBindByName(column = "review")
  String text;

  /**
   * The review polarity: positive/negative.
   */
  @CsvBindByName(column = "sentiment")
  String sentiment;

  /**
   * The dominant emotion of the review.
   */
  String emotion;

  /**
   * Array holding the tokens extracted from the text.
   */
  String[] tokens;

  /**
   * The POS tags for the tokens.
   */
  String[] tags;

  /**
   * Map containing a collection of emotions indexed by adjectives(their position in the tokens
   * array) found in the text.
   */
  MultiValueMap adjectiveToEmotionsMap;

  /**
   * Map containing the polarity scores of adjectives indexed by their position in the tokens array
   */
  Map<Integer, Double> adjectivePolarities;

  Double computedTextPolarity;

  /**
   * @return a list of pairs<adjectiveWord, positionInTokenList>
   */
  public List<Pair<String, Integer>> getAdjectiveWords() {
    List<Pair<String, Integer>> toReturn = IntStream.range(0, tokens.length)
        .filter(i -> tags[i].startsWith("J")).mapToObj(i -> Pair.of(tokens[i], i))
        .collect(Collectors.toList());

    return toReturn;
  }

  public void addEmotion(Integer key, String emotion) {
    if (this.adjectiveToEmotionsMap == null) {
      this.adjectiveToEmotionsMap = new MultiValueMap();
    }

    adjectiveToEmotionsMap.put(key, emotion);
  }

  public boolean isWordNegated(final Integer adjectivePos) {
    if (adjectivePos == null) {
      return false;
    }

    if (adjectivePos > 0 && adjectivePos <= tokens.length) {
      return tokens[adjectivePos - 1].equals("no")
          || tokens[adjectivePos - 1].equals("not")
          || tokens[adjectivePos - 1].equals("nor");
    }

    return false;
  }


  public void addAdjectivePolarity(Integer key, String adjectivePolarity) {
    if (adjectivePolarities == null) {
      this.adjectivePolarities = new HashMap();
    }

    adjectivePolarities.put(key, Double.valueOf(adjectivePolarity));
  }

  public String computePredominantEmotion() {
    Map<String, Integer> emotionCounters = PRIMITIVE_EMOTIONS.stream()
        .collect(Collectors.toMap(sentiment -> sentiment, sentiment -> 0));
    emotionCounters.put("neutral", 0);
    adjectiveToEmotionsMap.entrySet()
        .stream()
        .forEach(entry -> {
          Map.Entry<Integer, Collection<String>> mapEntry
              = ((Map.Entry<Integer, Collection<String>>) entry);
          for (String emotion : mapEntry.getValue()) {
            if (isWordNegated(mapEntry.getKey())) {
              String opposite = computeOpposite(emotion);
              int crtVal = emotionCounters.get((String) opposite);
              crtVal += 1;
              emotionCounters.put(opposite, crtVal);
            } else {
              int crtVal = emotionCounters.get((String) emotion);
              crtVal += 1;
              emotionCounters.put(emotion, crtVal);
            }
          }
        });

    Integer maxValue = emotionCounters.values().stream().map(integer -> integer)
        .max(Integer::compareTo).orElse(0);
    String emotionToReturn = emotionCounters.entrySet()
        .stream()
        .filter(entry -> entry.getValue().equals(maxValue))
        .map(Map.Entry::getKey)
        .reduce((k1, k2) -> k1 + "/" + k2)
        .orElse("UNDEFINED");
    return tuneEmotionAccordingToPolarity(emotionToReturn, computedTextPolarity);
  }

  private String tuneEmotionAccordingToPolarity(String emotionToReturn,
      Double computedTextPolarity) {
    String tuned = new String(emotionToReturn);
    if (computedTextPolarity < 0) {
      tuned = emotionToReturn.replace("admiration", "");
      tuned = tuned.replace("joy", "");
    }

    if( computedTextPolarity > 0) {
      tuned = tuned.replace("disgust", "");
      tuned = tuned.replace("sadness", "");
    }      tuned = tuned.replace("//","/");

    tuned = tuned.replace("//","/");

    return tuned;
  }

  /**
   * gets the opposite of an emotion according to their place on the wheel of emotions
   * joy <-> sadness
   * admiration <-> disgust
   * anger <-> fear
   * interest <-> surprise
   */
  private String computeOpposite(String emotion) {
    switch (emotion) {
      case "joy":
        return "sadness";
      case "sadness":
        return "joy";
      case "admiration":
        return "disgust";
      case "disgust":
        return "admiration";
      case "anger":
        return "fear";
      case "fear":
        return "anger";
      case "interest":
        return "surprise";
      case "surprise":
        return "interest";
      default:
        return "neutral";
    }
  }

  public Double computePolarity() {
    this.computedTextPolarity = adjectivePolarities.entrySet()
        .stream()
        .map(entry -> isWordNegated(entry.getKey()) ? -entry.getValue() : entry.getValue())
        .reduce(Double::sum)
        .orElse(Double.valueOf("0.0"));
    return computedTextPolarity;
  }

}
