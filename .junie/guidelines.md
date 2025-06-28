# Junie's Instructions for the Project

- Dependencies should be managed, updated, or added to the `libs.version.toml` file in the root of the project.
- Include KDoc (Kotlin documentation) for all public APIs and classes, which are not located under an `internal`
  package.
- Public APIs should be well-documented, with clear descriptions of their purpose, parameters, and return values.
- Suggest improvements to the codebase, such as refactoring opportunities, performance enhancements, or code
  organization.
- The project is written in Kotlin, use Kotlin 2.2 features when applicable. No Java code is allowed. Refactor Java code
  to Kotlin when applicable.
- The project does not allow warnings or deprecated APIs in the codebase, ensure there are no warnings.
- Follow the project's coding style and conventions.
- Try not to use try catch paradigm, Use kotlin alternatives to avoid the code smell that comes with try catch.
- Always check detekt and verify we are passing the lint when we make any change. This **ALWAYS** should be the very last step in the process.
- Double bangs (!!) are NOT allowed in any condition, Even when a null check is present before using double bang. Always
  use null checks for defensive programming.
