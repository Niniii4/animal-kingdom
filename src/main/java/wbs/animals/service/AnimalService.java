package wbs.animals.service;

import org.apache.jena.rdf.model.Resource;
import wbs.animals.model.Animal;
import wbs.animals.model.AnimalTypes;

import java.io.IOException;

public interface AnimalService {
    Animal getAnimal(String animal) throws IOException;

    void createAnimalType(Resource animalResource, AnimalTypes animalModel);
}
