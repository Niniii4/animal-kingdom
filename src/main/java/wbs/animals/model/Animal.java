package wbs.animals.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Animal {
    private String name;
    private String description;
    private String image;
    private List<AnimalTypes> animalType = new ArrayList<>();
}
