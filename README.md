# word2vec_pipeline

A Java pipeline for NLP that offers fast preprocessing and connects it to the [deeplearning4j](https://www.github.com/deeplearning4j/deeplearning4j)  Word2Vec implementation.


## Example Usage

### Preprocessing

```
ParallelPreProcessor<MyProcessor> pp = new ParallelPreProcessor<>(MyProcessor.class);
pp.processFile("corpus.txt", "processed.txt");
```

### Creating and training Word2Vec

```
// Create parameters object
Word2VecParams params = new Word2VecParams("modelName")
    .setMinWordFrequency(10)
    .setLayerSize(200);
// Set other Word2Vec parameters here ^

// Create a model with params 
Word2VecModel model = Word2VecModel.initializeWithParams(params);

// Train the model
Word2VecModel.trainModel(model, "processed.txt");

// Save the model to disk. The boolean argument is useful in keeping you from
// accidentally overwriting your models and is best kept as false
Word2VecModel.saveModel(model, "path/to/model.model", false);
```

### Evaluating Word2Vec

```
// Read the model from disk
Word2VecModel model = Word2VecModel.readModelByPath("path/to/model.model", "modelName");

// "king" - "man" + "woman" = "queen"
List<String> positiveWords = Arrays.asList("king", "woman");
List<String> negativeWords = Arrays.asList("man");
int topResultsToGet = 10;

List<BetterModelUtils.ScoredLabel> results = model.getClosest(positiveWords, negativeWords, topResultsToGet);

// Should be queen
System.out.println("closest word: " + results.get(0).getLabel);
System.out.println("similarity: " + results.get(0).getScore);
```

** You can also create an AnalogyTest which is easier **

```
AnalogyTest myTest = new AnalogyTest("king", "man", "woman", "queen");
TestResults results = myTest.run(model);

// Returns a message informing the results of the test
System.out.println(results.getMessage);

// This returns k where "queen" is the kth closest vector in the model to the result
System.out.println(results.getScore);
```

** Tests can be run in bulk as well **

```
Tester tester = new Tester();
List<Test> tests;

tests = AnalogyTest.readAnalogyTests("path/to/tests/file");
tester.runTestsOnModel(model, tests, new PrintWriter("path/to/output/file"));
```
