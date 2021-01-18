import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

@UtilityClass
public class Utils {

  /**
   * NLP utils
   */
  public static ChunkerME CHUNKER;
  public static POSTaggerME POS_TAGGER;
  /**
   * Ontology Utils
   */
  public static OWLOntology ONTOLOGY;
  public static Map<String, OWLNamedIndividual> INDIVIDUALS_CACHE;
  private static List<String> STOP_WORDS;

  public static void setUpOntologyAndIndividualsCache()
      throws OWLOntologyCreationException {
    OWLOntologyManager man = OWLManager.createOWLOntologyManager();
    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    File file = new File("/home/user/rcim/test/src/main/resources/ontosenticnet.owl");
    OWLOntology ontology = man.loadOntologyFromOntologyDocument(file);
    Map<String, OWLNamedIndividual> individualsCache = new HashMap();

    ontology.getClassesInSignature().stream()
        .filter(cls -> cls.getIRI().getFragment().equals("SenticConcept")).forEach(cls -> {
      OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
      NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(cls, true);
      instances.getFlattened().forEach(
          individual -> individualsCache.put(individual.getIRI().getFragment(), individual));
    });
    INDIVIDUALS_CACHE = individualsCache;
    ONTOLOGY = ontology;
  }

  public static List<Review> beanBuilderExample(int numberOfReviews) throws Exception {
    Reader reader = new BufferedReader(
        new FileReader("/home/user/rcim/test/src/main/resources/selectedReviews.csv"));
    CsvToBean<Review> cb = new CsvToBeanBuilder(reader)
        .withType(Review.class)
        .withSeparator(',')
        .withIgnoreLeadingWhiteSpace(true)
        .build();

    return cb.parse().stream().limit(numberOfReviews).collect(Collectors.toList());
  }

  public static void loadStopWords() throws IOException {
    STOP_WORDS = Files
        .readAllLines(Paths.get("/home/user/rcim/test/src/main/resources/stopWords.txt"));
  }

  public static void loadPosTaggingModel() throws IOException {
    InputStream is = new FileInputStream(
        "/home/user/rcim/test/src/main/resources/en-pos-maxent.bin");
    POSModel posModel = new POSModel(is);
    POS_TAGGER = new POSTaggerME(posModel);
  }

  public static void loadChunkingModel() throws Exception {
    InputStream is = new FileInputStream("/home/user/rcim/test/src/main/resources/en-chunker.bin");
    ChunkerModel chunkerModel = new ChunkerModel(is);
    CHUNKER = new ChunkerME(chunkerModel);
  }

  public static void processReview(Review review) {
    String[] tokenArray = Utils.cleanTextAndTokenize(review);
    review.setTokens(tokenArray);

    String tags[] = Utils.POS_TAGGER.tag(tokenArray);
    review.setTags(tags);
  }

  private static String[] cleanTextAndTokenize(Review review) {
    String reviewText = review.getText();

    String noHtmlTagsString = Jsoup.parse(reviewText).text();
    String noPunctuationString = noHtmlTagsString.replaceAll("\\p{P}", " ");
    String noNumbersString = noPunctuationString.replaceAll("\\w*\\d\\w*", "");

    List<String> tokens = Arrays
        .stream(StringUtils.normalizeSpace(noNumbersString).toLowerCase().split(" ")).collect(
            Collectors.toList());
    tokens.removeAll(STOP_WORDS);

    String[] tokenArray = new String[tokens.size()];
    tokenArray = tokens.toArray(tokenArray);
    return tokenArray;
  }

}
