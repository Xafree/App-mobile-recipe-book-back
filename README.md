# Gymgod - Backend

Version JAVA : JDK 25

## 📖 Architecture & Principes de Fonctionnement

Le backend est conçu selon les principes de la **Clean Architecture** / **Architecture Hexagonale**. Ce choix permettra plus tard d'extraire facilement des modules sous forme de microservices. Cette architecture impose une séparation stricte entre les modèles de données métier purs et les modèles d'API ou de base de données.

Voici à quoi ressemble l'arborescence détaillée (le "prototype") si l'on zoome à l'intérieur d'une fonctionnalité :

```text
com.entreprise.projet.<module>
├── domain/                             <-- 1. LE CŒUR MÉTIER (Zéro framework, pur Java)
│   ├── model/
│   │   ├── <Entity>.java               (Ex: Order. Entité métier principale)
│   │   ├── <SubEntity>.java            (Ex: OrderLine. Sous-entité ou Value Object)
│   │   └── <Entity>Status.java         (Enumérations métier)
│   ├── port/                           (Interfaces d'accès vers/depuis l'extérieur)
│   │   └── <Entity>Repository.java     (Interface définissant un besoin, ex: sauvegarde)
│   └── exception/                      
│       └── <Entity>NotFoundException.java (Erreurs purement métier)
│
├── service/                            <-- 2. APPLICATION / CAS D'USAGE (Orchestration)
│   ├── Create<Entity>UseCase.java      (Un fichier = une action précise)
│   ├── Update<Entity>UseCase.java
│   └── Run<Module>EtlUseCase.java      (Orchestration de traitements lourds/batch)
│
├── entrypoint/                         <-- 3. ADAPTEURS ENTRANTS (Exposition / Déclencheurs)
│   ├── api/
│   │   └── <Entity>Controller.java     (Endpoints REST : @RestController, @PostMapping)
│   ├── scheduler/                      
│   │   └── <Module>Scheduler.java      (Tâches planifiées : @Scheduled / Cron)
│   ├── event/                          
│   │   └── <Entity>EventListener.java  (Écouteurs de messages Kafka/RabbitMQ)
│   ├── dto/                            (Objets de transfert de données)
│   │   ├── request/
│   │   │   └── <Action><Entity>Request.java (Ce que l'appelant envoie)
│   │   └── response/
│   │       └── <Entity>Response.java   (Ce que l'appelant reçoit)
│   └── mapper/
│       └── <Entity>EntrypointMapper.java (Transforme les DTO en objets du Domain, et inversement)
│
├─ gateway/                            <-- 4. ADAPTEURS SORTANTS (Infrastructure / BDD / API externes)
|   ├── database/
|   │   ├── entity/
|   │   │   └── <Entity>JpaEntity.java  (Modèle de BDD avec @Table, @Column)
|   │   ├── repository/
|   │   │   └── SpringData<Entity>Repository.java (Interface Spring Data JPA)
|   │   └── adapter/
|   │       ├── <Entity>DatabaseAdapter.java (Implémente le port domain.port.<Entity>Repository)
|   │       └── <Module>EtlAdapter.java     (Exécute les requêtes natives massives d'extraction/chargement)
|   ├── external/                       (Appels vers d'autres modules ou API externes)
|   │   └── <ExternalService>Adapter.java (Implémente un port du domaine pour requêter l'extérieur)
|   └── mapper/
|       └── <Entity>GatewayMapper.java  (Transforme les entités JPA/Externes en objets du Domain)
├── config/                             <-- 5. LA COLLE (Framework & Beans)
│   ├── <Module>UseCaseConfig.java      (Déclare les UseCases comme des Beans Spring)
│   ├── <Module>SecurityConfig.java     (Configuration des filtres/rôles du module)
│   └── <Module>DatabaseConfig.java     (Optionnel : Configuration Hibernate/Flyway spécifique)
│
```

- **domain/ (Le Cœur Métier)** : Ce qu'il contient : Tes entités métiers, tes règles de gestion pures, et les interfaces de tes gateways (Ports).
  > **La règle d'or** : Ne doit dépendre d'absolument RIEN d'autre. Pas de framework, pas de dépendances à Spring, pas de SQL. Que du pur Java.

- **service/ (L'Application / Use Cases)** : Ce qu'il contient : L'orchestration. C'est ici que tu appelles ton domaine et tes gateways pour accomplir une action métier précise.
  > **La règle d'or** : Il connaît le domaine, mais ne connaît pas les détails d'implémentation de la base de données ou du web.

- **controller/ (Exposition / Adapteurs Entrants)** : Ce qu'il contient : Tes API REST (Controllers Spring MVC), tes écouteurs d'événements Kafka/RabbitMQ.
  > **La règle d'or** : Son seul rôle est de recevoir une requête HTTP/Event, de la traduire, et d'appeler le dossier `service/`.

- **gateway/ (Infrastructure / Adapteurs Sortants)** : Ce qu'il contient : L'implémentation de l'accès aux données (Repositories JPA, requêtes SQL) et les appels vers des API externes.
  > **La règle d'or** : C'est ici que se trouve le code technique lié à la persistance. Ce dossier implémente les interfaces définies dans ton `domain/`.

## 📊 ETL (Extract, Transform, Load)

Un accès ETL est défini pour récupérer la base de données nutritionnelle volumineuse.

- **Source ETL entrée** : [OpenFoodFacts](https://world.openfoodfacts.org/)
- **Lien des données :** [Télécharger le CSV GZ (1 Go compressé, ~9 Go décompressé)](https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz)

```text
CSV Data Export
Data for all products, or some of the products, can be downloaded in the CSV format (readable with LibreOffice, Excel and many other spreadsheet software) through the advanced search form.
```

## 🚀 Endpoints et API REST

L'application expose plusieurs ensembles d'API séparées par module métier.

### 1. Authentification (`/api`)

Gère l'inscription, la modification du profil et la sécurisation des sessions.

| Méthode | Endpoint                     | Description |
| ------- | ---------------------------- | ----------- |
| `GET`   | `/api/user`                  | Récupère les informations courantes de l'utilisateur authentifié |
| `POST`  | `/api/auth/register`         | Inscription d'un nouvel utilisateur (email, username, password) |
| `POST`  | `/api/auth/verify`           | Vérification de l'adresse email depuis un code reçu |
| `POST`  | `/api/auth/resend-code`      | Renvoie un code de vérification à l'utilisateur |
| `POST`  | `/api/auth/change-email`     | Change l'adresse e-mail de l'utilisateur avant sa vérification |

### 2. Nutrition & Produits Aliments (`/nutrition`)

Recherche et consultation des produits et de leurs nutriments.

| Méthode | Endpoint                                          | Description |
| ------- | ------------------------------------------------- | ----------- |
| `GET`   | `/nutrition/{key}`                                | Recherche détaillée d'un produit (ex: par code-barres) |
| `GET`   | `/nutrition/search`                               | Recherche de produits alimentaires (Query `name`, `page`, `size`) |
| `GET`   | `/nutrition/image/{code}/{filename}`              | Sert une image locale de produit (si mise en cache ou existante) |
| `DELETE`| `/nutrition/ingredients/cleanup`                  | Tâche d'administration : nettoyage des ingrédients orphelins (limité par `limit`) |
| `POST`  | `/nutrition/admin/maintenance/deduplicate-ingredients` | Tâche d'administration : Dédoublonne les ingrédients dans l'application |

### 3. Recettes de cuisine (`/api/v1/recipes`)

Permet de regrouper des aliments sous forme de recettes personnalisées pour calculer leurs données nutritionnelles.

| Méthode | Endpoint                     | Description |
| ------- | ---------------------------- | ----------- |
| `GET`   | `/api/v1/recipes`            | Liste toutes les recettes de l'utilisateur connecté |
| `GET`   | `/api/v1/recipes/favorites`  | Renvoie l'ensemble des recettes mises en favoris |
| `GET`   | `/api/v1/recipes/{id}`       | Récupère les informations détaillées d'une recette |
| `POST`  | `/api/v1/recipes`            | Création d'une nouvelle recette valide |
| `PUT`   | `/api/v1/recipes/{id}`       | Met à jour une recette existante existante |
| `DELETE`| `/api/v1/recipes/{id}`       | Supprime définitivement une de ses propres recettes |

### 4. Suivi des Repas (`/nutrition/meals`)

Gestion des logs nutritionnels journaliers (repas consommés) de l'utilisateur.

| Méthode | Endpoint                     | Description |
| ------- | ---------------------------- | ----------- |
| `GET`   | `/nutrition/meals`           | Liste les repas de l'utilisateur (Filtre possible par date : `?date=YYYY-MM-DD`) |
| `GET`   | `/nutrition/meals/{id}`      | Détail d'un repas particulier défini par l'identifiant |
| `POST`  | `/nutrition/meals`           | Crée un journal d'une nouvelle consommation (repas) |
| `PUT`   | `/nutrition/meals/{id}`      | Ajuste ou met à jour une consommation (quantité, aliments) |
| `DELETE`| `/nutrition/meals/{id}`      | Supprime un repas de l'historique |

### 5. Administration des Pays (`/admin/api/countries`)

Gestion de la régionalisation des données de l'application.

| Méthode | Endpoint                       | Description |
| ------- | ------------------------------ | ----------- |
| `GET`   | `/admin/api/countries`         | Liste paginée de tous les pays supportés |
| `PUT`   | `/admin/api/countries/{id}`    | Mise à jour des informations ou des variantes d'un pays |
| `GET`   | `/admin/api/countries/export`  | Exporte l'ensemble des pays au format CSV |
| `POST`  | `/admin/api/countries/import`  | Importe des pays cibles depuis un fichier CSV (Multipart) |
