# Contributing to velarc-spring-boot-starter

## Commit Format

This project uses Conventional Commits (https://www.conventionalcommits.org/). Every commit message must be structured as: <type>(<scope>): <description>

### Types

- feat — a new feature
- fix — a bug fix
- docs — documentation only changes
- chore — maintenance tasks (deps, CI, tooling)
- refactor — code change that neither fixes a bug nor adds a feature
- test — adding or correcting tests

Breaking changes must use ! after the type/scope (e.g., feat(client)!: remove deprecated method) or include a BREAKING CHANGE: footer.

### Examples

- feat(client): add retry support for transient failures
- fix(config): correct default timeout value
- chore: update Spring Boot to 3.5.x

## File Staging

Only commit and push the files you changed. Do not use git add . or git add -A — add files individually.

## Build Verification

Run mvn verify in the background (it may exceed bash timeout). Check the output file for results rather than re-running.

## Tests

Every code change must include appropriate tests. Unit tests for all classes with logic. Integration tests where Spring context wiring matters. No code lands without tests.

## Java Version

Source and target: Java 17. Do not use Java 21+ features.

## Dependencies

This is a library, not an application. Dependencies that customers don't need at runtime should be provided or test scope. Keep the dependency footprint minimal — every dependency we add is a dependency the customer inherits.

## No Force Push

Never force-push to main.

## Records

Use Java records for immutable data carriers (DTOs, value objects) where appropriate.

## Documentation

Update README.md when public API surface changes.

## License

By contributing, you agree that your contributions will be licensed under the Apache License, Version 2.0.
