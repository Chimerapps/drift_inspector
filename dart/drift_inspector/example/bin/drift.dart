import 'package:drift/drift.dart';

part 'drift.g.dart';

@DataClassName('Category')
class Categories extends Table {
  IntColumn get id => integer().autoIncrement()();

  TextColumn get description => text().nullable()();

  BoolColumn get exampleBool => boolean()();
}

class Recipes extends Table {
  IntColumn get id => integer().autoIncrement()();

  TextColumn get title => text().withLength(max: 16)();

  TextColumn get instructions => text()();

  IntColumn get category => integer().nullable()();
}

class Ingredients extends Table {
  IntColumn get id => integer().autoIncrement()();

  TextColumn get name => text()();

  IntColumn get caloriesPer100g => integer().named('calories')();
}

class IngredientInRecipes extends Table {
  @override
  String get tableName => 'recipe_ingredients';

  // We can also specify custom primary keys
  @override
  Set<Column> get primaryKey => {recipe, ingredient};

  IntColumn get recipe => integer()();

  IntColumn get ingredient => integer()();

  IntColumn get amountInGrams => integer().named('amount')();
}

@DriftAccessor(tables: [Ingredients, Categories, Recipes, IngredientInRecipes])
class ExampleDao extends DatabaseAccessor<MyDatabase> with _$ExampleDaoMixin {
  ExampleDao(MyDatabase db) : super(db);

  Stream<List<Recipe>> getRecipes() {
    return select(recipes).watch();
  }

  Future<void> replaceRecipes(List<Recipe> newRecipes) {
    return transaction(() async {
      await delete(recipes).go();

      await batch((batch) {
        batch.insertAll(recipes, newRecipes);
      });
    });
  }

  Future<void> replaceCategories(List<CategoriesCompanion> newCategories) {
    return transaction(() async {
      await delete(categories).go();

      await batch((batch) {
        batch.insertAll(categories, newCategories);
      });
    });
  }
}

@DriftDatabase(tables: [
  Categories,
  Recipes,
  Ingredients,
  IngredientInRecipes
], queries: {
  // query to load the total weight for each recipe by loading all ingredients
  // and taking the sum of their amountInGrams.
  'totalWeight': '''
      SELECT r.title, SUM(ir.amount) AS total_weight
        FROM recipes r
        INNER JOIN recipe_ingredients ir ON ir.recipe = r.id
      GROUP BY r.id
     '''
}, daos: [
  ExampleDao
])
class MyDatabase extends _$MyDatabase {
  MyDatabase(QueryExecutor e) : super(e);

  @override
  int get schemaVersion => 1;

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      beforeOpen: (details) async {
        // populate data
        await into(categories).insert(const CategoriesCompanion(
            description: Value('Sweets'), exampleBool: Value(true)));
      },
    );
  }
}
