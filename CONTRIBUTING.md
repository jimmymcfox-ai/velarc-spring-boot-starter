# Contributing to velarc-spring-boot-starter

Thank you for your interest in contributing!

## Commit Messages

This project follows the [Conventional Commits](https://www.conventionalcommits.org/) specification. Every commit message must be structured as:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types

- **feat** — a new feature
- **fix** — a bug fix
- **docs** — documentation only changes
- **chore** — maintenance tasks (deps, CI, tooling)
- **refactor** — code change that neither fixes a bug nor adds a feature
- **test** — adding or correcting tests

### Examples

```
feat(client): add retry support for transient failures
fix(config): correct default timeout value
chore: update Spring Boot to 3.5.x
```

## Building

```bash
mvn clean verify
```

## License

By contributing, you agree that your contributions will be licensed under the Apache License, Version 2.0.
