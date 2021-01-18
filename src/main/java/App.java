import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.search.EntitySearcher;

/**
 * Alphabetical list of part-of-speech tags used in the Penn Treebank Project:
 * No|Tag|Description
 *
 * 1.	  CC	Coordinating conjunction
 * 2.	  CD	Cardinal number
 * 3.	  DT	Determiner
 * 4.	  EX	Existential there
 * 5.	  FW	Foreign word
 * 6.	  IN	Preposition or subordinating conjunction
 * 7.	  JJ	Adjective
 * 8.   JJR	Adjective, comparative
 * 9.	  JJS	Adjective, superlative
 * 10.	LS	List item marker
 * 11.	MD	Modal
 * 12.	NN	Noun, singular or mass
 * 13.	NNS	Noun, plural
 * 14.	NNP	Proper noun, singular
 * 15.	NNPS	Proper noun, plural
 * 16.	PDT	Predeterminer
 * 17.	POS	Possessive ending
 * 18.	PRP	Personal pronoun
 * 19.	PRP$	Possessive pronoun
 * 20.	RB	Adverb
 * 21.	RBR	Adverb, comparative
 * 22.	RBS	Adverb, superlative
 * 23.	RP	Particle
 * 24.	SYM	Symbol
 * 25.	TO	to
 * 26.	UH	Interjection
 * 27.	VB	Verb, base form
 * 28.	VBD	Verb, past tense
 * 29.	VBG	Verb, gerund or present participle
 * 30.	VBN	Verb, past participle
 * 31.	VBP	Verb, non-3rd person singular present
 * 32.	VBZ	Verb, 3rd person singular present
 * 33.	WDT	Wh-determiner
 * 34.	WP	Wh-pronoun
 * 35.	WP$	Possessive wh-pronoun
 * 36.	WRB	Wh-adverb
 */
public class App {

  public static void main(String[] args) throws Exception {
    /*
     * Primitive emotions in ontology: admiration, anger, disgust, fear, interest, joy,
     * sadness, surprise. Look for these in the list of attributes of an individual
     * Combine these with the polarity and polarity score of a word to determine the main primitive emotion
     */
    // Set up ontology and individuals cache
    Utils.setUpOntologyAndIndividualsCache();

    // Read reviews to be processed
    List<Review> reviews = Utils.beanBuilderExample(10);

    // load nlp processing tools
    Utils.loadStopWords();
    Utils.loadPosTaggingModel();
    Utils.loadChunkingModel();

    // NLP processing
    reviews.forEach(Utils::processReview);

    // Text analysis processing
    reviews.forEach(review -> {
      System.out
          .println("\n-------------------------------------------------------------------------");
      System.out.println(review.getText());
      List<Pair<String, Integer>> adjectiveWords = review.getAdjectiveWords();
      adjectiveWords.forEach(adjective -> {
        OWLNamedIndividual individual = Utils.INDIVIDUALS_CACHE.get(adjective.getLeft());
        if (individual != null) {
          Set<OWLAnnotation> adjectiveAnnotations =
              (Set<OWLAnnotation>) EntitySearcher.getAnnotations(individual, Utils.ONTOLOGY);

          // Extract primitive emotions associated with the adjective
          adjectiveAnnotations.stream()
              .filter(annotation -> annotation.getProperty().toString().contains("#primitiveURI"))
              .map(annotation -> annotation.getValue().asIRI().get().getFragment())
              .forEach(emotion -> review.addEmotion(adjective.getRight(), emotion));

          // Extract the adjective polarity
          adjectiveAnnotations.stream()
              .filter(annotation -> annotation.getProperty().toString().contains("#polarity"))
              .filter(annotation -> !annotation.getProperty().toString().contains("#polarityText"))
              .map(annotation -> annotation.getValue().asLiteral().get().getLiteral())
              .forEach(adjectivePolarity -> review
                  .addAdjectivePolarity(adjective.getRight(), adjectivePolarity));
        }
      });
      System.out.println(review.computePolarity());
      System.out.println(
          review.computePredominantEmotion() + "(real polarity: " + review.getSentiment() + ")");
      System.out
          .println("-------------------------------------------------------------------------\n");
    });

  }
}
