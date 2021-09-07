package wbs.animals.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import wbs.animals.model.Animal;
import wbs.animals.service.AnimalService;

import java.io.IOException;

@Controller
@RequestMapping("/api")
public class AnimalController {
    public final AnimalService animalService;

    public AnimalController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @RequestMapping(value = "/animals")
    public String getHomePage() {
        return "animal";
    }

    @GetMapping("/animals/animalType")
    public String getAnimal(Model model, @RequestParam String name) throws IOException {
        Animal animal = animalService.getAnimal(name);
        model.addAttribute("animalTypes", animal);
        return "animalType";
    }
}
