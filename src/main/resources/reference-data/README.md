# Données de référence CIQUAL / USDA

La table `reference_foods` (entité `ReferenceFood`) est alimentée
automatiquement au démarrage de l'application par
`ReferenceFoodImportStartupRunner` / `ReferenceFoodImportService`, à partir
de deux jeux de données officiels et gratuits, téléchargés (puis réutilisés
tant qu'ils ont moins de 7 jours) :

- **CIQUAL** (ANSES, France/UE) — table de composition nutritionnelle
  complète (~3484 aliments, fichier Excel 1 ligne/aliment).
- **USDA FoodData Central** — jeu "Foundation Foods" (US), fourni en CSV
  relationnels dans un ZIP.

Ces données servent au job hebdomadaire `ReferenceFoodEnrichmentJobAdapter`
pour compléter les `Nutriment`/`Glucide` des produits OFF bruts/génériques
dont les macros sont manquantes (`nutritionDataIncomplete=true`, sans
marque), par correspondance de nom (pg_trgm) sur `reference_foods.name`.

## Sources et configuration (`application.properties`)

```properties
path.ciqual.url=https://ciqual.anses.fr/cms/sites/default/files/inline-files/Table%20Ciqual%202025_FR_2025_11_03.xls
path.ciqual.file=data/ciqual.xls
path.usda.url=https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_foundation_food_csv_2026-04-30.zip
path.usda.file=data/usda_foundation.zip
```

- `path.ciqual.url` pointe vers le fichier Excel "Table Ciqual <année>" de
  l'édition CIQUAL en vigueur, publié directement par l'ANSES sur
  https://ciqual.anses.fr (page "Télécharger la table Ciqual"). À mettre à
  jour lors d'une nouvelle édition CIQUAL (nouveau nom de fichier).
  `CiqualImportService` utilise `WorkbookFactory.create(...)`, qui détecte
  automatiquement le format `.xls` (binaire, format actuel de l'ANSES) ou
  `.xlsx` — adapter `path.ciqual.file` selon l'extension réelle du fichier.
  Le portail `entrepot.recherche.data.gouv.fr` (Dataverse) a été abandonné :
  son redirection vers S3 (`opens3r-tls.stockage.inrae.fr`) échoue côté JVM
  avec une erreur `PKIX path building failed` (certificat non reconnu par le
  truststore Java), alors que `curl`/le système l'acceptent.
- `path.usda.url` est une URL **datée** (`FoodData_Central_foundation_food_csv_<YYYY-MM-DD>.zip`,
  cf. https://fdc.nal.usda.gov/download-datasets). USDA publie une nouvelle
  version "Foundation Foods" périodiquement — mettre à jour la date dans
  cette URL pour suivre une nouvelle publication.

## Import et upsert

`ReferenceFoodImportService.importAll()` :
1. Télécharge (ou réutilise) les deux fichiers via `ReferenceFoodFileService`.
2. `CiqualImportService` parse le fichier Excel CIQUAL (en-têtes retrouvés par
   correspondance de libellés, valeurs `g/100g` et `mg/100g`).
3. `UsdaImportService` extrait `food.csv` / `food_nutrient.csv` /
   `food_category.csv` du ZIP USDA et reconstruit les valeurs par 100g pour
   chaque aliment `data_type = foundation_food`.
4. Chaque aliment est upserté dans `reference_foods` par
   `(source, source_code)` — idempotent, pas de duplication au redémarrage.
