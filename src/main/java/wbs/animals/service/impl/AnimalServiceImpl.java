package wbs.animals.service.impl;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.riot.RDFParser;
import org.springframework.stereotype.Service;
import wbs.animals.model.Animal;
import wbs.animals.model.AnimalTypes;
import wbs.animals.service.AnimalService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnimalServiceImpl implements AnimalService {
    public static String URL = "http://dbpedia.org/data/animal.ttl";

    public static void queryAnimals(String animal, Animal animalModel) {
        String queryString = "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                "PREFIX dbr: <http://dbpedia.org/resource/>\n" +
                "select  ?about\n" +
                "where{\n" +
                "dbr:ANIMAL dbo:abstract ?about\n." +
                "FILTER(lang(?about) = \"en\")\n" +
                "}";
        queryString = queryString.replace("ANIMAL", animal);
        Query query = QueryFactory.create(queryString);
        QueryExecution queryExecution = QueryExecutionFactory.sparqlService("https://dbpedia.org/sparql", query);
        ResultSet resultSet = queryExecution.execSelect();
        while (resultSet.hasNext()) {
            QuerySolution querySolution = resultSet.nextSolution();
            String[] solution = querySolution.toString().split("\"");
            String description = solution[1];
            animalModel.setDescription(description);
        }
    }

    public static List<String> getBirdsUrl() {
        List<String> birdsUrl = new ArrayList<>();
        String birdQueryString = "PREFIX dbr: <http://dbpedia.org/resource/>\n" +
                "select  ?birdType\n" +
                "where{\n" +
                "?birdType <http://purl.org/linguistics/gold/hypernym> dbr:Bird.\n" +
                "}";
        Query birdQuery = QueryFactory.create(birdQueryString);
        QueryExecution birdQueryExecution = QueryExecutionFactory.sparqlService("https://dbpedia.org/sparql", birdQuery);
        ResultSet birdResultSet = birdQueryExecution.execSelect();
        while (birdResultSet.hasNext()) {
            QuerySolution birdQuerySolution = birdResultSet.nextSolution();
            String birdUrl = birdQuerySolution.toString().split("\"")[0].replace("( ?birdType = <", "").replace("> )", "");
            birdsUrl.add(birdUrl);
        }
        return birdsUrl;
    }

    @Override
    public Animal getAnimal(String animal) {
        Animal animalModel = new Animal();
        List<AnimalTypes> animalTypesResultList = new ArrayList<>();
        AnimalTypes animalTypesModel = new AnimalTypes();
        URL = URL.replace("animal", animal);

        int counter = 0;
        Model model = ModelFactory.createDefaultModel();
        RDFParser.source(URL).httpAccept("text/turtle").parse(model.getGraph());

        String resURL = URL.replace("data", "resource");
        URL = URL.replace(animal, "animal");
        resURL = resURL.replace(".ttl", "");

        Resource animalResource = model.getResource(resURL);
        createAnimal(animalResource, animal, animalModel);

        if (!animal.equals("Bird")) {
            Property animalTypeProperty = model.getProperty("http://purl.org/linguistics/gold/hypernym");
            ResIterator animalTypeIterator = model.listSubjectsWithProperty(animalTypeProperty);
            while (animalTypeIterator.hasNext()) {
                String animalUrl = animalTypeIterator.nextResource().getProperty(animalTypeProperty).getSubject().toString();
                Model modelUrl = ModelFactory.createDefaultModel();
                RDFParser.source(animalUrl).httpAccept("text/turtle").parse(modelUrl.getGraph());
                Resource animalTypeResource = modelUrl.getResource(animalUrl);

                createAnimalType(animalTypeResource, animalTypesModel);
                animalTypesModel = new AnimalTypes();
                animalTypesResultList.add(animalTypesModel);
                if ((animalModel.getName().equals("Fish") || animalModel.getName().equals("Mammals")) && counter > 300) {
                    break;
                }
                counter++;
            }
        } else {
            List<String> birdsUrl = getBirdsUrl();
            for (int i = 0; i < birdsUrl.size() && i < 300; i++) {
                Model birdModel = getModel(birdsUrl.get(i));
                Resource birdTypeResource = birdModel.getResource(birdsUrl.get(i));
                createAnimalType(birdTypeResource, animalTypesModel);
                animalTypesModel = new AnimalTypes();
                animalTypesResultList.add(animalTypesModel);
            }
        }

        List<AnimalTypes> animalTypes = animalTypesResultList.stream()
                .filter(a -> (a.getName() != null)
                        && ((a.getDescription() != null && !a.getDescription().trim().isEmpty())
                        && (a.getImage() != null && !a.getImage().trim().isEmpty())))
                .filter(a -> !a.getName().equals(animalModel.getName()))
                .limit(40)
                .sorted(Comparator.comparing(AnimalTypes::getImage, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        animalModel.setAnimalType(animalTypes);

        return animalModel;
    }

    private Model getModel(String animalUrl) {
        Model modelBook = ModelFactory.createDefaultModel();
        RDFParser.source(animalUrl).httpAccept("text/turtle").parse(modelBook.getGraph());
        return modelBook;
    }

    private void createAnimal(Resource animalResource, String animal, Animal animalModel) {
        String animalName = animalResource.getProperty(new PropertyImpl("http://dbpedia.org/property/name"), "en").getObject().toString().replace("@en", "");
        String thumbnail = animalResource.getProperty(new PropertyImpl("http://dbpedia.org/ontology/thumbnail")).getObject().toString();
        queryAnimals(animal, animalModel);
        animalModel.setName(animalName);
        animalModel.setImage(thumbnail);
    }

    @Override
    public void createAnimalType(Resource animalResource, AnimalTypes animalModel) {
        if (animalResource.hasProperty(new PropertyImpl("http://dbpedia.org/property/name"))) {
            String animalTypeName = animalResource.getProperty(new PropertyImpl("http://dbpedia.org/property/name")).getObject().toString().replace("@en", "");
            String animalTypeDescription = ((animalResource.hasProperty(new PropertyImpl("http://dbpedia.org/ontology/abstract")))
                    ? ((animalResource.getProperty(new PropertyImpl("http://dbpedia.org/ontology/abstract"), "en") != null)
                    ? animalResource.getProperty(new PropertyImpl("http://dbpedia.org/ontology/abstract"), "en").getObject().toString()
                    .replace("@en", "").split("\\* v")[0].trim() : "") : "");
            String animalTypeImage = (animalResource.hasProperty(new PropertyImpl("http://dbpedia.org/ontology/thumbnail"))
                    ? animalResource.getProperty(new PropertyImpl("http://dbpedia.org/ontology/thumbnail")).getObject().toString() : "");
            animalModel.setName(animalTypeName);
            animalModel.setDescription(animalTypeDescription);
            animalModel.setImage(animalTypeImage);
        }
    }
}
