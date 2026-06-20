package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Product;

/// Résultat d'un batch ETL — compteurs créés/mis-à-jour/ignorés + dernier produit traité.
public record BatchResult(int created, int updated, int skipped, Product lastProduct) {}
