package fr.gymgod.app.meal.controller;

import fr.gymgod.app.meal.domain.entites.record.MealCreateRecord;
import fr.gymgod.app.meal.domain.entites.record.MealRecord;
import fr.gymgod.app.meal.service.OrchestratorMeal;
import fr.gymgod.common.constants.ConstantsCommon;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ConstantsCommon.ENDPOINT_NUTRITION + "/meals")
@RequiredArgsConstructor
public class MealControllerAdapter {

    private final OrchestratorMeal orchestratorMeal;

    @GetMapping
    public ResponseEntity<List<MealRecord>> getUserMeals(@RequestParam(required = false) java.time.LocalDate date) {
        if (date != null) {
            return ResponseEntity.ok(orchestratorMeal.getMealsByDate(date));
        }
        return ResponseEntity.ok(orchestratorMeal.getUserMeals());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealRecord> getMeal(@PathVariable UUID id) {
        MealRecord meal = orchestratorMeal.getMeal(id);
        if (meal != null) {
            return ResponseEntity.ok(meal);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<MealRecord> createMeal(@RequestBody MealCreateRecord request) {
        return ResponseEntity.ok(orchestratorMeal.createMeal(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealRecord> updateMeal(@PathVariable UUID id, @RequestBody MealCreateRecord request) {
        MealRecord updatedMeal = orchestratorMeal.updateMeal(id, request);
        if (updatedMeal != null) {
            return ResponseEntity.ok(updatedMeal);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeal(@PathVariable UUID id) {
        orchestratorMeal.deleteMeal(id);
        return ResponseEntity.noContent().build();
    }
}
